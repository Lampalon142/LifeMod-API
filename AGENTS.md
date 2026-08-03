# LifeMod Agent Guide

## Workflow
- **Required order:** `./gradlew build shadowJar` then `./gradlew test`
- **Commit & push proactively:** Always take the initiative to commit and push any modification to `v2` with conventional commits — do NOT wait to be asked
- **No test classes exist** — `./gradlew test` produces 0% JaCoCo coverage; report is still generated and uploaded to Codecov
- **No lint/format/typecheck** rules configured
- **Java 21 toolchain** in `build.gradle`; CI runner uses JDK 17 (Temurin) — toolchain resolves the mismatch
- **12 subprojects** bundled into one fat JAR via ShadowJar: `api`, `common`, `platform:bukkit`, `platform:bungee`, `nms:abstraction`, and 7 version-specific NMS modules (`nms:v1_8_R3` through `nms:v1_21_R1`)
- **ShadowJar output:** `out/` (not `build/libs/`); regular JARs in `build/libs/`
- **Post-shadowJar hook** in root `build.gradle` copies the JAR to hardcoded dev-server plugin folders (won't work for other environments)
- **Modrinth token:** Passed via `-PmodrinthToken` in CI from `secrets.MODRINTH_TOKEN` — do NOT put in `gradle.properties`

## Architecture
- **DI:** Always `ServiceRegistry.get(Class)` — never `new` services directly downstream
- **`ServiceRegistry` is a static `HashMap`** — not thread-safe; single-threaded startup assumed
- **NMS:** `nms:abstraction` defines `NmsFactory` and `AbstractNmsHandler`; each `nms:vX_X_RX` module provides a `NmsHandlerImpl`. `NmsFactory` detects version from Bukkit package name first; falls back to `Bukkit.getBukkitVersion()` string for modern Paper/Purpur. No `net.minecraft.server` imports outside `nms/*` modules.
- **Cross-platform:** `ILifePlatform` for broadcast, kick, async — don't use Bukkit-specific APIs in shared code
- **Bukkit knows Java, Java doesn't know Bukkit:** Business logic must never depend on Bukkit APIs; only Bukkit code may import Bukkit classes. Exception: `NMSProvider` (in `common/nms/api`) accepts Bukkit types by design.
- **Sanctions:** Ban/kick/mute/warn via `ISanctionService`
- **Messages/Config:** Use `ILangService` / `IConfigurationService` — never read config or lang files directly
- **Lang fallback:** Key-by-key merge, falls back to embedded `en_US`
- **Bungee NMS:** `BungeePlatform.getNmsProvider()` returns `null` (no NMS on proxy)
- **Redis channels:** `lifemod:sanctions` (Bukkit + Bungee), `lifemod:staff` (Bukkit only) — via `IMessagingService` (Jedis)
- **Entrypoints:** `BukkitPlatform` stores `LifeMod` instance for NMS handlers that need it; `BungeePlatform` is lighter

## Commands
- Auto-discovered via reflection from JAR scanning (`CommandRegistry.scanAndRegisterCommands(...)`)
- Constructors accept `(LifeMod)` or `()`
- Commands live in `platform/bukkit/src/.../commands/impl/{admin,moderation,player,utility,world,report,trace,log}/`

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
- Bundled: PacketEvents 2.7.0 (separate spigot + bungeecord coordinates), InvUI, Jedis, HikariCP, jBCrypt, bStats

## CI/CD
- **Triggers:** Push/PR to `master`, `main`, `v2`, `dev`; release on tag `v*`
- **CI matrix:** JDK 17 + 21 (Temurin), `build shadowJar test` + Codecov upload
- **Release flow:** Push to `v2` → auto-bump patch version → `shadowJar` → GH Release (with auto-changelog) → `modrinth` publish
- **PR title convention:** `^(feat|fix|chore|refactor|docs|test|ci)(\(.+\))?: .{1,}` (enforced via PR checks)
- **CodeQL security scan** on every push/PR + weekly schedule
- **Dependabot** opens weekly PRs for Gradle + Actions updates
- **Modrinth token:** passed via `-PmodrinthToken` from `secrets.MODRINTH_TOKEN` (do NOT put in `gradle.properties`)
