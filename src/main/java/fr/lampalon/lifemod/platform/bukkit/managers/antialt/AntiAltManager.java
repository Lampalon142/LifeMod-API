package fr.lampalon.lifemod.platform.bukkit.managers.antialt;

import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.common.antialt.HeuristicEngine;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.entity.Player;

public class AntiAltManager {

    private final LifeMod plugin;
    private final HeuristicEngine engine;

    public AntiAltManager(LifeMod plugin) {
        this.plugin = plugin;
        this.engine = new HeuristicEngine(ServiceRegistry.get(IConfigurationService.class));
    }

    public void handlePlayerJoin(Player player) {
        if (!plugin.getConfigConfig().getBoolean("modules.antialt.enabled", false)) {
            return;
        }
        
        if (player.hasPermission("lifemod.antialt.bypass")) {
            return;
        }
        
        // Whitelist check would go here
        
        String ipAddress = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "127.0.0.1";
        
        AnalysisResult result = engine.analyze(player.getName(), ipAddress);
        
        // Execute reaction based on score
        plugin.getReactionManager().executeReactions(result, player);

        // Optional debug log
        if (plugin.getConfigConfig().getBoolean("modules.antialt.debug")) {
            plugin.getLogger().info("[AntiAlt Debug] Player: " + result.getPlayerName() + " | Score: " + result.getDangerScore() + " | Reason: " + result.getReason() + " | Fingerprint: " + result.getFingerprint());
        }
    }
}
