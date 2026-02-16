package fr.lampalon.lifemod.platform.bukkit.managers.antialt;

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
        if (!plugin.getConfigConfig().getBoolean("modules.antialt.enabled", true)) return;
        if (player.hasPermission("lifemod.antialt.bypass")) return;

        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "127.0.0.1";
        
        // Appel asynchrone du moteur
        engine.analyze(player.getName(), ip).thenAccept(result -> {
            // Retour sur le thread principal pour les réactions si nécessaire (géré dans ReactionManager)
            plugin.getReactionManager().executeReactions(result, player);
        });
    }
}
