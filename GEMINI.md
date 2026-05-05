# LifeMod Project Documentation

LifeMod is a comprehensive, multi-platform moderation plugin for Minecraft servers (Bukkit and BungeeCord). It provides a wide range of tools for staff, including sanctions, replays, anti-alt/VPN systems, and advanced staff mode management.

## Project Overview

*   **Main Technologies:** Java 17, Gradle.
*   **Platforms:** Bukkit (Spigot/Paper) and BungeeCord.
*   **Core Dependencies:**
    *   **PacketEvents:** Used for low-level packet handling (v2.7.0).
    *   **InvUI:** Powering the plugin's graphical user interfaces.
    *   **Redis (Jedis):** Facilitates cross-server messaging and synchronization.
    *   **Database:** Supports MySQL and SQLite using HikariCP for connection pooling.
    *   **Security:** jBCrypt for moderator authentication.
*   **Architecture:**
    *   `fr.lampalon.lifemod.common`: Contains shared logic, interfaces, and models.
    *   `fr.lampalon.lifemod.platform.bukkit/bungee`: Platform-specific implementations.
    *   `fr.lampalon.lifemod.common.nms`: Abstraction layer for Minecraft Version-specific code (NMS).
    *   `ServiceRegistry`: A centralized service locator for accessing core systems like databases, platforms, and services.

## Building and Running

*   **Build the project:** `./gradlew build`
*   **Generate Fat JAR (ShadowJar):** `./gradlew shadowJar` (JARs will be in `build/libs/`)
*   **Run Tests:** `./gradlew test`
*   **Test Coverage Report:** `./gradlew jacocoTestReport`

## Development Conventions

*   **NMS Abstraction:** NEVER use direct NMS imports (net.minecraft.server) or CraftBukkit imports in the `common` or general `platform` packages. Always use the `NMSProvider` interface and add version-specific implementations in `fr.lampalon.lifemod.platform.bukkit.nms.vX_X_RX`.
*   **Service Access:** Use `ServiceRegistry.get(Class<T>)` to retrieve services like `ILifePlatform`, `ISanctionService`, or `DatabaseProvider`.
*   **Code Style:**
    *   Strictly follow standard Java naming conventions.
    *   Only use JavaDoc comments (`/** ... */`) in the `api` modules or shared interfaces.
    *   Favor composition and abstraction (interfaces) to maintain multi-platform compatibility.
*   **Configurability:** All messages and settings MUST be configurable via `config.yml` and `lang.yml`. Use `IConfigurationService` and `ILangService`.
*   **Sanctions:** All sanctions (bans, mutes, kicks) should be handled through the `ISanctionService` to ensure consistency across platforms and database persistence.

## Key Modules & Features

*   **Replay System:** Located in `fr.lampalon.lifemod.common.replay`, it allows recording and replaying player actions.
*   **Anti-Alt & Anti-VPN:** Advanced heuristic-based systems for detecting alt accounts and proxy/VPN usage.
*   **Staff Mode:** A unified system for vanishing staff, providing moderation tools, and tracking staff actions.
*   **Reports:** A GUI-based reporting system for players and staff.
