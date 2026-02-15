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

        // Execute reaction based on score
        plugin.getReactionManager().executeReactions(result, connection);
        
        // For now, we will just log it. The ReactionManager needs to be adapted for Bungee.
        if (plugin.getConfig().getBoolean("modules.antialt.debug", false)) {
            plugin.getLogger().info("[AntiAlt Debug Bungee] Player: " + result.getPlayerName() + " | Score: " + result.getDangerScore() + " | Reason: " + result.getReason());
        }
    }
}
