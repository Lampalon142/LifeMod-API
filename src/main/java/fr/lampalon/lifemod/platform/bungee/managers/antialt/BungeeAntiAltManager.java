package fr.lampalon.lifemod.platform.bungee.managers.antialt;

import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.common.antialt.HeuristicEngine;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import fr.lampalon.lifemod.platform.bungee.utils.MessageUtil;
import net.md_5.bungee.api.connection.PendingConnection;

import java.util.stream.Collectors;

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
        engine.analyze(connection.getUniqueId(), connection.getName(), ipAddress).thenAccept(result -> {
            // Execute reaction based on score
            plugin.getReactionManager().executeReactions(result, connection);

            // Translatable Debug Log
            if (plugin.getConfig().getBoolean("modules.antialt.debug", false)) {
                ILangService lang = ServiceRegistry.get(ILangService.class);
                String rules = result.getTriggeredRules().stream()
                        .map(r -> lang.getMessage("antialt.rules." + r.getKey()))
                        .collect(Collectors.joining(", "));
                
                String debugMsg = lang.getMessage("antialt.debug",
                        "%player%", result.getPlayerName(),
                        "%score%", String.valueOf(result.getDangerScore()),
                        "%reason%", rules);
                
                plugin.getLogger().info(lang.formatMessage(debugMsg));
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}
