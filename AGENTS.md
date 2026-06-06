# LifeMod Agent Guide

## Workflow
- **Commit after every logical change** — never batch unrelated fixes in one commit
- **Required order:** `./gradlew build shadowJar` then `./gradlew test`
- **No tests exist** — `gradlew test` is a no-op; JaCoCo coverage always 0%
- **No lint/format/typecheck** rules configured
- **Java 21 toolchain** in `build.gradle`; CI runner uses JDK 17 (Temurin) — toolchain resolves the mismatch
- **Single-module project** — Bukkit + Bungee code live in one source set (no subprojects)
- **Artifacts:** `build/libs/*.jar`; publish requires `MODRINTH_TOKEN` env var

## Architecture
- **DI:** Always `ServiceRegistry.get(Class)` — never `new` services directly
- **NMS:** All version-specific code in `platform.bukkit.nms.*` only; no `net.minecraft.server` imports outside that package
- **Cross-platform:** `ILifePlatform` for broadcast, kick, async — don't use Bukkit-specific APIs in shared code
- **Bukkit knows Java, Java doesn't know Bukkit:** Business logic must never depend on Bukkit APIs; only Bukkit code may import Bukkit classes
- **Sanctions:** Ban/kick/mute/warn via `ISanctionService`
- **Messages/Config:** Use `ILangService` / `IConfigurationService` — never read config or lang files directly
- **Lang fallback:** Key-by-key merge, falls back to embedded `en_US`
- **Bungee NMS:** `BungeePlatform.getNmsProvider()` returns `null` (no NMS on proxy)

## Style
- **No superfluous comments** — code should be self-documenting
- **Always optimize** — prefer efficient, minimal code
- **Respect Java conventions** — follow standard naming, formatting, and idioms

## Commands
- Auto-discovered via reflection (`CommandRegistry.scanAndRegisterCommands(...)`)
- Constructors accept `(LifeMod)` or `()`
- **Duplicates exist:** `InvseeCommand`, `SpectateCommand`, `StaffHistoryCommand` appear in both `impl/player/` and `impl/moderation/` packages

## Config & Resources
- Bukkit: `config.yml` | Bungee: `bungee-config.yml`
- Lang files: `languages/<lang>.yml` (Bukkit), `languages/bungee_<lang>.yml` (Bungee)

## Dependencies
`implementation`-scope deps bundled via ShadowJar: PacketEvents 2.7.0, InvUI, Jedis (Redis), HikariCP, jBCrypt, bStats. Add new deps at `implementation` scope for ShadowJar inclusion.

## CI/CD
- **Triggers:** Push/PR to `master`, `main`, `v2`, `dev`
- **Release:** Tag `v*` → `shadowJar` → GH Release → Modrinth publish
- **⚠ Token in VCS:** Modrinth token hardcoded in `gradle.properties`

## Reference
- `GEMINI.md` — supplementary project overview
- `PostHogSetup.java` — standalone CLI to create all LifeMod PostHog dashboards/insights
- `PosthogClear.java` — standalone CLI to delete all LifeMod PostHog dashboards/insights (supports `--dry-run`)

---

## Session Progress (posthog-implementation)

### Goal
Finish all PostHog tracking improvements and fix remaining bugs in the staff mode / PIN / cross-server inventory system.

### Done
- **NPE in BungeeAntiAltManager**: added `if (result == null) return;`.
- **IndexOutOfBoundsException in AntiVPNPacketListener**: try-catch around wrapper creation.
- **PacketProcessException packet ID 310**: removed PacketEvents from Bungee entirely.
- **AntiVPN wrong name "m"**: created `BungeeAntiVPNListener` with native `PreLoginEvent`.
- **Redis subscribers**: added `lifemod:sanctions` (Bukkit + Bungee) and `lifemod:staff` (Bukkit) subscriptions.
- **Cross-server inventory fallback**: added global `getRawInventory(UUID)` / `deleteRawInventory(UUID)`.
- **`hasStaffItems` check removed** permanently — user explicitly said save must always happen.
- **Global `deleteRawInventory(UUID)` removed** — was destroying other servers' saves during restore.
- **`forceDisableOnJoin` fixed**: no longer clears inventory for normal players; only cleans up orphan saves from DB.
- **`setStaffModeState` made synchronous**: DB write now happens immediately, preventing race when switching servers.
- **ROOT CAUSE — `PlayerQuit` fix**: removed `data.setInStaffMode(false)` which was asynchronously overwriting the flag after the next server already read it.
- **PIN session timeout**: `last_auth_ip` + `last_auth_time` columns added to `moderator_auth` table; same IP + within 5 min → auto-authenticate from DB; different IP or >5 min → PIN required.
- **Auto-deploy JAR**: `build.gradle` now copies `out/LifeMod-*.jar` to Lobby/Proxy/Skyblock plugins after shadowJar.
- **PostHog events enriched**: `lifemod_sanction`/`lifemod_auto_punish`/`lifemod_sanction_pardon` now include `is_permanent`, `server_name`, `category`; `lifemod_player_quit` includes `server_name` and `is_staff`.
- **Persistent instance ID**: `server.id` file in plugin data folder, sent as `instance_id` on all events (avoids PostHog duplicate server entries across restarts).
- **`lifemod_mod_auth` enriched**: now includes `action` (login/register/session_restore), `success`, and `session_restored`.
- **`lifemod_error` capture utility**: added `captureError(String message, String context)` to `PostHogService` and `IPostHogService`.
- **Session restore tracking**: auto session-restore in `ModeratorAuthListener.onJoin()` now fires `lifemod_mod_auth` with `action=session_restore`.
- **Old tracking removed from ModeratorAuthService.verifyPin()**: dead code — was never called; proper tracking now lives in PinGui.validate().
