# LifeMod Agent Guide (High-Signal Instructions)

This guide contains operational constraints and conventions that agents must follow to work correctly on LifeMod. Adherence ensures stability and correct usage across platforms.

## 🛠 Build & Test Workflow
*   **Mandatory Order:** Always run `gradlew build shadowJar` first, followed by `./gradlew test`.
*   **Publishing:** Requires setting the `MODRINTH_TOKEN` environment variable. Artifacts are found in `build/libs/*.jar`.

## 🏗 Core Architecture Gotchas
1.  **Service Access (DI):** Never instantiate services directly. Always use `ServiceRegistry.get(Class)` to retrieve dependencies.
2.  **NMS Abstraction:** All Minecraft Network Stack (NMS) code *must* be isolated within the platform-specific handlers (`platform.bukkit.nms.*`). Do not introduce direct `net.minecraft.server` imports in common core logic.
3.  **Cross-Platform Operations:** Use `ILifePlatform` for all cross-cutting concerns (e.g., broadcast, kick, async tasks).

## ⚙️ Coding Conventions & Quirks
*   **Message/Settings Handling:** All user-facing text and configuration changes must flow through the dedicated services: `ILangService` or `IConfigurationService`.
*   **Sanctions:** Ban, kick, mute, and warn actions require using `ISanctionService` to maintain consistency.
*   **Command Discovery:** Commands are auto-discovered via reflection (`CommandRegistry.scanAndRegisterCommands(...)`). Understand that constructors accept either `(LifeMod)` or `()`.
*   **Config Files:** Bukkit uses `config.yml`; BungeeCord uses `bungee-config.yml`.

## 🚀 CI/CD & Release Flow
*   CI runs on: `master`, `main`, `v2`, `dev` (push + PR).
*   Release procedure: Tagging with `v*` triggers `shadowJar` build, GH release, and Modrinth publishing.
