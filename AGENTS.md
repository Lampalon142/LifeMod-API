# LifeMod — Agent Guide

## Build & Test

```bash
./gradlew build shadowJar      # full build + fat JAR
./gradlew test                  # unit tests
./gradlew jacocoTestReport      # coverage report (build/reports/jacoco/)
./gradlew modrinth              # publish to Modrinth (needs MODRINTH_TOKEN)
```

CI runs `build shadowJar` then `test`. Release via `v*` tag creates GH release + Modrinth upload.

## Architecture

- **Java 17**, single Gradle module, root `LifeMod` project.
- **Dual-platform**: Bukkit (`src/.../platform/bukkit/LifeMod.java`) and BungeeCord (`platform/bungee/BungeeLifeMod.java`).
- **Common core** at `fr.lampalon.lifemod.common.*`: models, services, database, replay, NMS API, anti-alt, anti-VPN.
- **Platform-specific** at `fr.lampalon.lifemod.platform.bukkit.*` / `platform.bungee.*`.
- **ServiceRegistry** singleton (`common.core.ServiceRegistry`): `register(Class, impl)` / `get(Class)` — used everywhere in place of DI.
- **NMS abstraction**: `NMSProvider` interface in `common.nms.api`, version implementations in `platform.bukkit.nms.v1_20_R1/`, `v1_21_R1/`. Loaded by `NMSLoader.load()`. No direct `net.minecraft.server` imports outside version handlers.
- **Commands**: extend `LifeCommand` (abstract) in `platform.bukkit.commands.api`. Auto-discovered via `CommandRegistry.scanAndRegisterCommands("...commands.impl")` using jar reflection. Constructors accept either `(LifeMod)` or `()`.
- **Dependencies**: PacketEvents 2.7, InvUI, HikariCP, Jedis, jBCrypt, bStats.
- **Languages**: `src/main/resources/languages/` — Bukkit (`en_US.yml`) and Bungee (`bungee_en_US.yml`). Merged at startup; `en_US` base used as fallback.

## Key Conventions

- NMS code **only** in `platform.bukkit.nms.*` handlers. Never in `common` or other platform packages.
- Services accessed via `ServiceRegistry.get(Class)` — never instantiated directly.
- All messages/settings go through `ILangService` / `IConfigurationService`.
- Sanctions (ban/kick/mute/warn) go through `ISanctionService` for consistency.
- Commands can be toggled via `config.yml > commands.enabled.<name>`.
- Config files: `config.yml` (Bukkit), `bungee-config.yml` (Bungee).
- Plugin.yml declares all commands; the custom `CommandRegistry` optionally wraps them.
- Always use `ILifePlatform` for cross-platform ops (broadcast, kick, async tasks).
- Gradle wrapper committed (`gradlew`, `gradlew.bat`).

## CI/CD

- Branches with CI: `master`, `main`, `v2`, `dev`.
- Release workflow: tag `v*` triggers shadowJar + GH release + Modrinth publish.
- Modrinth project ID: `K3tLtfl0`, loaders: spigot/paper, version: 1.20.1.
- bStats plugin ID: 19817.
- Artifact: `build/libs/*.jar` (shadowJar, minimized, relocates libs to `fr.lampalon.lifemod.libs.*`).
