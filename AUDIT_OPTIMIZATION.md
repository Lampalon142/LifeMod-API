 # Optimization Audit Report

Audit date: 2026-05-27
Branch: `feature/optimization-audit`
Principle: *Bukkit knows Java, Java doesn't know Bukkit* — business logic must never depend on Bukkit APIs.

---

## 🔴 Critical Issues

### 1. N+1 Sanction Queries in Loops

**Files:** `PlayerJoin.java:98-109`, `AltsCommand.java:60-68`, `AltsGui.java:64`, `HeuristicEngine.java:55-78`

For each alt account on an IP, a separate `getActiveSanction()` query fires synchronously. No batch API (`WHERE uuid IN (...)`) exists in `DatabaseProvider` — it only exposes single-UUID methods. This forces callers into N+1.

**Fix:** Add `getActiveSanctions(Collection<UUID>)` to `DatabaseProvider` and use `WHERE uuid IN (?)`.

### 2. Replay File I/O on Main Thread

**Files:** `ReplaySession.java:69-73`, `ReplayCommand.java:109-110`

`ReplaySession.stop()` writes all buffered frames to disk synchronously on the calling thread — which is the **main server thread** (called from `ReplayAutoStartListener.onQuit`, via `PlayerQuitEvent`). Same for `ReplayCommand.load`: `BinaryReplayReader.readAllFrames()` reads the entire file synchronously on the command thread.

**Fix:** Offload I/O to `CompletableFuture.supplyAsync(...)` or a dedicated executor.

### 3. SQLite Single Shared Connection — No Thread Safety

**File:** `SQLiteManager.java:14,55-58`

A single `Connection` instance is shared across all threads without synchronization. `connection.isClosed()` is a race condition; two concurrent async tasks can both see `null`, both call `connect()`, losing one connection reference. SQLite JDBC will throw `SQLITE_BUSY`.

**Fix:** Use HikariCP for SQLite too, or synchronize access / use `SQLiteDataSource`.

### 4. Entity/Player Leaks — Maps Never Cleaned on Quit

| File | Maps | Impact |
|------|------|--------|
| `LifeMod.java:81` + `CPSListener` | `cpsMap` | Every player that clicks leaves a `Deque<Long>` entry forever. Grows unbounded. |
| `FreezeManager.java:15-16` | `playerHelmets`, `frozenPlayers` | Frozen players who quit without being unfrozen leak ItemStack + Location references. |
| `SpectateManager.java:13-16` | `originalLocations`, `lastTargets`, `isFreecam`, `spectateTarget` | Every staff who spectates without `/spectate leave` leaks entries in all 4 maps. |
| `NoteInputManager.java:19` | `pendingInputs` | Players disconnected while typing a note leave their `NoteContext` behind. |
| `StaffListener.java:39` | `lastActionTick` | Every unique staff player that interacts leaves a Long entry. |

**Fix:** Register `PlayerQuitEvent` handlers in each manager to clean up entries.

---

## 🟠 High Severity

### 5. Bukkit Imports in Common Code (Architecture Violation)

| File | Offending Imports |
|------|-------------------|
| `common/replay/Interpolator.java` | `org.bukkit.Location`, `org.bukkit.util.Vector` |
| `common/service/IItemsAdderService.java` | `org.bukkit.inventory.ItemStack` |
| `common/nms/api/NMSReplayHandler.java` | `org.bukkit.Location`, `org.bukkit.entity.Player` |
| `common/nms/api/NMSProvider.java` | `org.bukkit.World`, `org.bukkit.block.Container`, `org.bukkit.entity.Player` |

**Note:** `Interpolator` and `EntityIdMapper` are entirely **dead code** — never called anywhere.

### 6. getAllReports() Without Pagination

**Files:** `MySQLManager.java:123-129`, `SQLiteManager.java:106-112` → called from `TicketJoinListener.java:32`, `ReportsCommand.java:49`, `GuiDetailListener.java:62`

`SELECT * FROM reports ORDER BY created_at DESC` with no LIMIT. Called on **every player join** who has `lifemod.report.join` permission, on the **main thread**.

**Fix:** Add LIMIT + OFFSET, or paginate. Cache counts for the join notification badge.

### 7. Synchronous DB on Main Thread

**File:** `PlayerQuit.java:28-38` — Three synchronous SQL queries (`getPlayerData`, `savePlayerData`, `saveCoords`) inside `PlayerQuitEvent`, which fires on the **main server thread**. Blocks all world activity during player disconnect.

**Fix:** Wrap DB calls in `runTaskAsynchronously`.

### 8. Race Condition: NPE on `getPlayer()` After `isOnline()` Check

**Files:** `BanCommand.java:36`, `KickCommand.java:39`, `MuteCommand.java:27,33-35`, `WarnCommand.java:27,33-35`

Pattern: `target.isOnline()` checked → `target.getPlayer()` used later. Between the check and the use, the player can disconnect, making `getPlayer()` return null. No null guard exists.

**Fix:** Capture `Player` reference before the online check, or add null guards after `getPlayer()`.

### 9. Tab Completer Rebuilds Player List Every Invocation

**Files:** `TabCompleterUtils.java:24-27`, `CompletionUtil.java:18-25`

Every tab-complete event creates a new `Stream<Player>`, maps to names, and collects into a new `List<String>`. On large servers, typing one character triggers this O(n) scan.

**Fix:** Cache player names with refresh on join/quit events (via `PlayerJoinEvent` / `PlayerQuitEvent`).

### 10. Empty Catch Blocks (11 occurrences)

| File | Lines |
|------|-------|
| `PlaybackManager.java` | 144, 159, 197, 207, 213, 250 |
| `StaffListener.java` | 167 |
| `NMSHandler_v1_21_R1.java` | 194 |
| `ScanManager.java` | 203, 337, 368 |

Exceptions silently swallowed. Makes debugging impossible.

**Fix:** Log each exception at minimum, or restructure to avoid throwing.

### 11. ReplayPlayerManager Maps Leak on Disconnect

**File:** `ReplayPlayerManager.java:20-21` — `savedStates` and `activePlaybacks` maps never cleaned when a player disconnects while in replay mode. No `PlayerQuitEvent` listener.

**Fix:** Add quit handler to restore player state and remove maps entries.

---

## 🟡 Medium Severity

### 12. HikariCP Missing Timeout/SSL/Validation Config

**File:** `MySQLManager.java:30-36`

Only `maximumPoolSize` and `poolName` are set beyond credentials. Missing:
- `setConnectionTimeout()` / `setIdleTimeout()` / `setMaxLifetime()`
- `setLeakDetectionThreshold()`
- `useSSL=false` hardcoded with no config toggle
- No `serverTimezone` (MySQL 8+ can throw timezone errors)

### 13. Replay Position Recorder Allocates Every Tick

**File:** `ReplayPositionRecorder.java:27-58`

Runs every tick (50ms). Per-player allocation: 2 stream objects + 1 `byte[]` + 1 `ReplayFrame` + 1 `Collections.singletonList` = **5+ allocations per player per tick**. At 20 TPS × 50 players = 5,000+ allocs/sec.

**Fix:** Pool byte arrays, reduce allocation in hot path, or decrease recording frequency for position data.

### 14. PlaybackManager VIRTUAL_ID_COUNTER Never Resets

**File:** `PlaybackManager.java:45`

Static `AtomicInteger VIRTUAL_ID_COUNTER` starts at 900_000 and increments for every playback-ever started. On long-lived servers with frequent replay usage, this will eventually wrap past `Integer.MAX_VALUE` to `Integer.MIN_VALUE`, colliding with real server entity IDs.

**Fix:** Reset per-playback or use a different ID scheme.

### 15. ServiceRegistry Lookups Per Command Execution

**Files:** `BukkitCommandAdapter.java:26`, `BukkitCommandWrapper.java:34`

`ServiceRegistry.get(ILangService.class)` called on **every** command execution. Hashmap lookup is fast, but the instance never changes at runtime — could be injected once in the constructor.

**Fix:** Store `ILangService` as a field set in constructor.

### 16. ChatClearCommand Sends 100 Empty Messages Per Player

**File:** `ChatclearCommand.java:23-28`

Loop sending 100 empty `""` messages to **every** online player. On a server with 100 players, that's 10,000 `sendMessage` calls on the main thread.

**Fix:** Use `Bukkit.broadcast(Component.text("\n".repeat(100)))` or similar one-shot approach.

### 17. `recording` Field Not Volatile

**File:** `ReplaySession.java:29`

`boolean recording` read from netty threads and written from main thread without `volatile`. Netty threads may see a stale `true` after `stop()` sets it to `false`.

**Fix:** Add `volatile` modifier.

### 18. SkinManager Caches Grow Unbounded

**File:** `SkinManager.java:27-29`

Two `ConcurrentHashMap` caches (UUID → TextureProperty, name → TextureProperty) accumulate every player ever seen. No eviction policy, no size cap.

**Fix:** Add cache eviction (e.g., Guava Cache or scheduled cleanup).

### 19. Inefficient Collection Choices

| File | Current | Suggested |
|------|---------|-----------|
| `StaffActionManager.java:22` | `HashMap<StaffActionType, IStaffAction>` | `EnumMap` (array-backed, lower overhead) |
| `SpectateManager.java:15` | `Map<UUID, Boolean>` | `Set<UUID>` (Boolean always true when present) |

### 20. PacketListener Instances Not Storable for Individual Cleanup

**Files:** `NoClipManager.java:24-25`, `CPSListener` (local var in LifeMod.java:304-306), `VanishPacketListener`, `ReplayPacketListener`, `AntiVPNPacketListener`

All packet listeners registered without storing a reference. Relies entirely on `PacketEvents.getAPI().terminate()` in `onDisable()`. If that call fails or is removed, these listeners leak across plugin reloads.

---

## 🔷 CompletableFuture Analysis

### Issue A: `.join()` / `.get()` on Main Thread Defeats Async Purpose

`SanctionService` correctly wraps DB queries in `CompletableFuture.supplyAsync()`, but 4+ callers immediately call `.join()` **on the main server thread**, blocking it until the ForkJoinPool executes the query. This is **strictly worse than synchronous code** — adds thread-scheduling overhead while still blocking.

| File | Line | Problem |
|------|------|---------|
| `PlayerJoin.java:102` | Main thread (PlayerJoinEvent) | `.join()` blocks main thread |
| `AltsGui.java:64` | Main thread (GUI render) | `.join()` blocks main thread |
| `AltsCommand.java:66` | Main thread (command) | `.join()` blocks main thread |
| `SanctionListener.java:31,62` | Async thread (PreLogin/Chat) | `.get()` is OK (already async) but inconsistent style |
| `ConnectionListener.java:54` | Async thread (PreLogin) | `.join()` is OK (already async) |

**Fix:** Replace `.join()` / `.get()` with `.thenAccept()` chaining. Async events (`AsyncPlayerPreLoginEvent`, `AsyncPlayerChatEvent`) can keep blocking `.join()` since they're already off-main-thread, but should add a timeout.

### Issue B: No Timeout on Any `.join()` / `.get()`

Every blocking call uses `.join()` or `.get()` with no timeout parameter. If the database or an HTTP provider hangs, the calling thread hangs **indefinitely**:
- `SanctionListener.java:31` — `.get()` no timeout → main thread of async event hangs
- `PlayerJoin.java:102` — `.join()` no timeout → server freezes
- `HeuristicEngine.java:59,99` — `.join()` inside `supplyAsync` → ForkJoinPool thread hangs
- `StaffHistoryCommand.java:37` — `.join()` in async task → async thread hangs

**Fix:** Use `.get(5, TimeUnit.SECONDS)` with timeout and handle `TimeoutException`.

### Issue C: ForkJoinPool Common Pool Starvation

All `CompletableFuture.supplyAsync()` calls use `ForkJoinPool.commonPool()` (the default). This pool is shared with parallel streams and other JVM internals. Heavy operations run here:

| Operation | Location | Risk |
|-----------|----------|------|
| DB queries | `SanctionService.java:33,78,156,161,166` | Block common pool threads |
| HTTP requests (IP API) | `IPApiProvider.java:17` | Block for up to 5s |
| Anti-alt analysis | `HeuristicEngine.java:38` | Long CPU + blocking `.join()` |
| Inventory scanning | `ScanManager.java:41` | Heavy world/chunk iteration |

**Fix:** Create a dedicated `ExecutorService` (e.g., `Executors.newCachedThreadPool()`) and pass it to `supplyAsync()`.

### Issue D: `IPLookupManager` — Potential Pending Lookup Leak

**File:** `IPLookupManager.java:26-48`

If all providers fail and the last `lookupFromProviders` call completes normally (line 53), it calls `pendingLookups.remove(ip)`. **But** if an exception occurs **after** `.thenAccept()` fires but **before** `future.complete(info)` executes (e.g., in `db.saveIPInfo()` at line 63), the future never completes and `pendingLookups.remove()` never runs. The `pendingLookups` entry leaks permanently.

**Fix:** Move `pendingLookups.remove(ip)` into a `future.whenComplete()` block, or use a `try/finally`.

### Issue E: Missing `exceptionally()` in Async Chains

| File | Chain | Problem |
|------|-------|---------|
| `SanctionService.java:174` | `.thenAccept(history -> ...)` | Auto-punish failures silently swallowed |
| `AntiVPNPacketListener.java:36` | `.thenAccept(allowed -> ...)` | VPN lookup failures have no error handler |
| `BungeeAntiAltManager.java:32` | `.thenAccept(result -> ...)` | Analysis failures invisible |
| `AntiAltManager.java:52` | `.thenAccept(result -> ...)` | Analysis failures invisible |

**Fix:** Add `.exceptionally(ex -> { logger.warn(...); return null; })` to every async chain.

### Issue F: No Dedicated Executor

Every async operation uses either `ForkJoinPool.commonPool()` (via `supplyAsync`) or Bukkit/Bungee's scheduler (`runTaskAsynchronously`). There is no central `Executor` / thread pool for plugin operations:

- Bukkit scheduler limits async tasks per tick (can backlog)
- ForkJoinPool is shared JVM-wide
- No way to monitor or limit plugin thread usage

**Fix:** Create a `LIFEMOD_EXECUTOR` thread pool in `LifeMod` / `BungeeLifeMod` and inject it into services via `ServiceRegistry`.

**Summary:** The `CompletableFuture` wrapper in `SanctionService` is a good abstraction, but 80% of callers misuse it by blocking on `.join()` — making the code slower than a simple synchronous call.

---

## Summary

| Category | 🔴 Critical | 🟠 High | 🟡 Medium |
|----------|-----------|---------|-----------|
| Architecture (Bukkit in common) | — | 4 files | — |
| Database | 1 (N+1) | 2 (pagination, SQLite sync) | 1 (HikariCP config) |
| Main thread blocking | — | 3 (PlayerQuit DB, replay I/O) | — |
| Entity/player leaks | — | 5 managers | — |
| NPE race conditions | — | 4 commands | — |
| Tab completion | — | 2 files | — |
| Empty catch blocks | — | 11 | — |
| Replay system | 2 (I/O on main) | 1 (maps leak) | 5+ |
| CompletableFuture misuse | — | 4 (`.join()` on main thread) | 5+ (no timeout, no executor, no error handling) |
| Code quality | — | — | 6+ |

**Total: 3 critical, 15+ high, 15+ medium findings.**
