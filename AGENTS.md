# LifeMod Agent Guide

## Build & Test
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
