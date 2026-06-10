# LifeMod Agent Guide

## Workflow
- **Required order:** `./gradlew build shadowJar` then `./gradlew test`
- **No test classes exist** — `./gradlew test` produces 0% JaCoCo coverage; report is still generated and uploaded to Codecov
- **No lint/format/typecheck** rules configured
- **Java 21 toolchain** in `build.gradle`; CI runner uses JDK 17 (Temurin) — toolchain resolves the mismatch
- **3 subprojects** (`common`, `platform:bukkit`, `platform:bungee`) bundled into one fat JAR via ShadowJar
- **ShadowJar output:** `out/` (not `build/libs/`); regular JARs in `build/libs/`
- **Post-shadowJar hook** in root `build.gradle` copies the JAR to hardcoded dev-server plugin folders (won't work for other environments)
- **Modrinth token hardcoded in plaintext** in `gradle.properties:1` — publish via `./gradlew modrinth` (requires `MODRINTH_TOKEN` env var or the file value)

## Architecture
- **DI:** Always `ServiceRegistry.get(Class)` — never `new` services directly downstream
- **`ServiceRegistry` is a static `HashMap`** — not thread-safe; single-threaded startup assumed
- **NMS:** All version-specific code in `platform.bukkit.nms.*` only; no `net.minecraft.server` imports outside that package
- **`NMSLoader` detects version** from Bukkit package name first; falls back to `Bukkit.getBukkitVersion()` string for modern Paper/Purpur
- **Cross-platform:** `ILifePlatform` for broadcast, kick, async — don't use Bukkit-specific APIs in shared code
- **Bukkit knows Java, Java doesn't know Bukkit:** Business logic must never depend on Bukkit APIs; only Bukkit code may import Bukkit classes
- **Sanctions:** Ban/kick/mute/warn via `ISanctionService`
- **Messages/Config:** Use `ILangService` / `IConfigurationService` — never read config or lang files directly
- **Lang fallback:** Key-by-key merge, falls back to embedded `en_US`
- **Bungee NMS:** `BungeePlatform.getNmsProvider()` returns `null` (no NMS on proxy)
- **Redis channels:** `lifemod:sanctions` (Bukkit + Bungee), `lifemod:staff` (Bukkit only) — via `IMessagingService` (Jedis)
- **Entrypoints:** `BukkitPlatform` stores `LifeMod` instance for NMS handlers that need it; `BungeePlatform` is lighter

## Commands
- Auto-discovered via reflection (`CommandRegistry.scanAndRegisterCommands(...)`)
- Constructors accept `(LifeMod)` or `()`
- **Duplicates exist:** `InvseeCommand`, `SpectateCommand`, `StaffHistoryCommand` appear in both `impl/player/` and `impl/moderation/` packages

## Style
- **No superfluous comments** — code should be self-documenting
- **Always optimize** — prefer efficient, minimal code
- **Respect Java conventions** — follow standard naming, formatting, and idioms

## Config & Resources
- Bukkit: `config.yml` | Bungee: `bungee-config.yml`
- Lang files: `languages/<lang>.yml` (Bukkit), `languages/bungee_<lang>.yml` (Bungee)
- `plugin.yml` / `bungee.yml` use Gradle token expansion (`$VERSION`)

## Dependencies
- `implementation`-scope deps bundled via ShadowJar; add new deps at `implementation` for inclusion
- Bundled: PacketEvents 2.7.0 (separate spigot + bungeecord coordinates), InvUI, Jedis, HikariCP, jBCrypt, bStats, PostHog SDK
- PostHog API key **XOR-obfuscated** in source via `PostHogObfuscation` (`OBFUSCATED_KEY_HEX`); not in config files

## PostHog Analytics
- SDK config: debug=true, flushInterval=5s, flushAt=1 (aggressive flush)
- **Persistent instance ID:** `server.id` file in plugin data folder (survives restarts, avoids duplicate PostHog server entries)
- `captureError(String message, String context)` utility on `IPostHogService`

## Standalone CLIs
- `PostHogSetup.java` — creates all LifeMod PostHog dashboards/insights
- `PosthogClear.java` — deletes all LifeMod PostHog dashboards/insights (supports `--dry-run`)

## CI/CD
- **Triggers:** Push/PR to `master`, `main`, `v2`, `dev`; release on tag `v*`
- **Release flow:** Tag `v*` → `shadowJar` → GH Release → `modrinth` publish
