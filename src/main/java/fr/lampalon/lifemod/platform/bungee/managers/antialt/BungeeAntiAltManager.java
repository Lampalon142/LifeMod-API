package fr.lampalon.lifemod.platform.bungee.managers.antialt;

import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.common.antialt.HeuristicEngine;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import net.md_5.bungee.api.connection.PendingConnection;

public class BungeeAntiAltManager {

    private final BungeeLifeMod plugin;
    private final HeuristicEngine engine;

    public BungeeAntiAltManager(BungeeLifeMod plugin) {
        this.plugin = plugin;
        this.engine = new HeuristicEngine(ServiceRegistry.get(IConfigurationService.class));
    }

    public void handleConnection(PendingConnection connection) {
        if (!plugin.getConfig().getBoolean("modules.antialt.enabled", false)) {
            return;
        }

        // Whitelist check would go here

        String ipAddress = connection.getSocketAddress().toString();
        AnalysisResult result = engine.analyze(connection.getName(), ipAddress);

        // On Bungee, we can't easily run player-specific commands,
        // so we mainly focus on logging, alerting staff via Redis/messaging, or kicking.
        // The Bukkit-side listeners will handle in-game actions like mute/freeze.

        // For now, we will just log it. The ReactionManager needs to be adapted for Bungee.
        if (plugin.getConfig().getBoolean("modules.antialt.debug")) {
            plugin.getLogger().info("[AntiAlt Debug Bungee] Player: " + result.getPlayerName() + " | Score: " + result.getDangerScore() + " | Reason: " + result.getReason());
        }
        
        // Example: kick if score is too high
        if (result.getDangerScore() > 85) {
            String reason = "High-threat account detected. Please contact staff if this is an error.";
            connection.disconnect(new net.md_5.bungee.api.chat.TextComponent(reason));
        }
    }
}
