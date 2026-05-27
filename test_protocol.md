# Test Protocol — Optimization Audit

**Branch:** `feature/optimization-audit`
**Commit:** `9ad39e5`
**Build:** `./gradlew build shadowJar` ✔

---

## 1. Compilation & Build

| Test | Command | Expected |
|------|---------|----------|
| Compile | `./gradlew compileJava` | BUILD SUCCESSFUL |
| ShadowJar | `./gradlew shadowJar` | BUILD SUCCESSFUL, `build/libs/LifeMod-*-all.jar` created |
| Full build | `./gradlew build shadowJar` | BUILD SUCCESSFUL |

---

## 2. N+1 Sanction Queries (Fix #1)

**Setup:** Join with 5+ alt accounts on same IP.

| Step | Action | Expected |
|------|--------|----------|
| 1 | Player A (with 5 alts) joins | `getActiveSanctions()` called once with 5 UUIDs, not 5 separate queries |
| 2 | `/alts <PlayerA>` | Single batch query, alt list displayed |
| 3 | `/alts gui <PlayerA>` | GUI opens, each alt shows ban status from pre-computed map |

**Verify in logs:** Only 1 SQL `WHERE player_uuid IN` per call, no repeated `WHERE player_uuid = ?` loops.

---

## 3. Replay File I/O (Fix #2)

**Setup:** Plugin loaded, replay enabled.

| Step | Action | Expected |
|------|--------|----------|
| 1 | Start replay recording | Position recorder runs every 2 ticks (was 1) |
| 2 | Replay auto-stops on quit | File write happens in `CompletableFuture.runAsync`, server doesn't freeze |
| 3 | `/replay load <file>` | File read in async thread, GUI opens without blocking |

---

## 4. SQLite Thread Safety (Fix #3)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Set `database.type: SQLITE` in config | Plugin loads without error |
| 2 | 5 players join simultaneously | `getConnection()` is synchronized, no `SQLITE_BUSY` |
| 3 | Concurrent DB operations (join + command + anti-alt check) | No race conditions, no connection loss |

---

## 5. Entity Leaks (Fix #4 + #11)

**Setup:** 5 online players, each with different states.

| Step | Action | Expected |
|------|--------|----------|
| 1 | Freeze Player A | `/freeze A` — A is frozen |
| 2 | Spectate Player B | `/spectate B` — staff is spectating |
| 3 | Open note input for Player C | `/note C add` — typing state is tracked |
| 4 | Player A quits | FreezeManager.handleQuit() removes from frozenPlayers + playerHelmets |
| 5 | Player B quits | SpectateManager.handleQuit() clears all 4 maps |
| 6 | Player C quits | NoteInputManager.handleQuit() removes pendingInputs |
| 7 | Staff D interacts once and quits | StaffListener.handleQuit() removes lastActionTick |
| 8 | Player E with vanish quits | VanishService.handleQuit() removes from vanishedPlayers |
| 9 | All moves | cpsMap gets cleaned on quit |

**Verify after 10 min uptime:** Memory usage is stable (no leak accumulation).

---

## 6. Database Pagination (Fix #6)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Create 200+ reports | Reports exist in DB |
| 2 | Player with `lifemod.report.join` joins | `getAllReports(100, 0)` returns 100, not 200 |
| 3 | `/reports` | 100 reports displayed |
| 4 | Click "back" in report GUI | `getAllReports(100, 0)` again |

**Verify in logs:** SQL contains `LIMIT 100 OFFSET 0`.

---

## 7. Async PlayerQuit (Fix #7)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Player with data in DB quits | `savePlayerData`, `saveCoords` run async |
| 2 | Measure server TPS during quit | No TPS drop (DB write is off main thread) |

---

## 8. NPE Race Conditions (Fix #8)

| Step | Action | Expected |
|------|--------|----------|
| 1 | `/ban PlayerOffline` | No NPE, command handles null Player gracefully |
| 2 | `/kick PlayerOffline` | No NPE |
| 3 | `/mute PlayerOffline` | No NPE |
| 4 | `/warn PlayerOffline` | No NPE |
| 5 | Player disconnects exactly between command parsing and execution | `target.getPlayer()` returns null, handled gracefully |

---

## 9. Empty Catch Blocks (Fix #10)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Trigger playback error in replay | Stack trace printed to console (was silently swallowed) |
| 2 | Trigger NMS error | Stack trace printed (was silently swallowed) |
| 3 | Trigger scan error | Stack trace printed (was silently swallowed) |

---

## 10. Tab Completer Cache (Fix #9)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Type `/ ` and press Tab | Player names appear from cache (<1s refresh) |
| 2 | Player joins/quit | Tab list updates within 1 second |
| 3 | Rapid typing (`/msg a` then `b` then `c`) | No repeated Bukkit.getOnlinePlayers() calls |

**Verify in profiler:** `Player#getName` calls are limited to ~1/sec, not per-keystroke.

---

## 11. HikariCP Config (Fix #12)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Set `database.type: MYSQL` | Plugin connects with configured pool |
| 2 | Check HikariCP logs | Connection timeout 5s, leak detection 10s, idle 10min, max lifetime 30min |
| 3 | Set `database.use-ssl: true` | JDBC URL includes `useSSL=true` |

---

## 12. CompletableFuture Async (Fixes A-F)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Run `/history <player>` | SanctionService runs on LIFEMOD_EXECUTOR, not ForkJoinPool |
| 2 | Trigger auto-punish check | Exception in chain is logged via `.exceptionally()` |
| 3 | Player with VPN joins | `.exceptionally()` catches any VPN lookup failure |
| 4 | Sanction check during login | `.get(5, SECONDS)` — timeouts after 5s if DB hangs |
| 5 | Concurrent IP lookups | `pendingLookups` deduplicates correctly, no leak |

---

## 13. Architecture — IItemsAdderService (Fix #5)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Use ItemsAdder items in `/scan` | Resolves correctly from new `platform.bukkit.adapter` package |
| 2 | Plugin loads | No `ClassNotFoundException` for old `common.service.IItemsAdderService` |

---

## 14. ServiceRegistry Field Injection (Fix #15)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Run any command | `BukkitCommandAdapter.execute()` uses pre-injected `ILangService` field, not `ServiceRegistry.get()` |
| 2 | Performance check | 1 less HashMap.get() per command execution |

---

## 15. Memory — SkinManager (Fix #18)

| Step | Action | Expected |
|------|--------|----------|
| 1 | 1000+ unique players join over time | Skin cache auto-clears when >1000 entries (every 30 min) |
| 2 | Check console logs | Cleanup task runs silently |

---

## 16. ID Counter — PlaybackManager (Fix #14)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Start 2 replays simultaneously | Each `PlaybackManager` instance has its own ID counter starting at 900_000 |
| 2 | Verify entity ID collision risk | `static` removed — no wrap-around issue across playbacks |

---

## Quick Smoke Test (run after deploy)

```bash
# 1. Build
./gradlew build shadowJar

# 2. Deploy JAR to server's plugins/ folder

# 3. Start server
java -jar server.jar

# 4. Run these in-game:
/lifemod
/lifemod reload
/freeze <player>
/spectate <player>
/note <target> add test
/ban <target> test
/kick <target> test
/mute <target> test
/warn <target> test
/history <target>
/alts <target>
/reports
/replay start
/replay stop
/replay load <latest>
```

---

## Regression Risk Areas

| Area | Risk | Mitigation |
|------|------|------------|
| PlayerQuit rewrite | Data not saved if async fails | Wrapped in try/catch with stack trace |
| AltsGui constructor change | Old callers missing ban map | All 3 instantiations updated |
| getAllReports signature | Callers still pass no args | All 4 callers updated to pass (100, 0) |
| IItemsAdderService move | ClassLoader can't find it | Import path updated in all 4 referrers |
| SanctionService executor | Thread leak if plugin reloaded | Executor daemon threads auto-terminate |
