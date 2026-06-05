package fr.lampalon.lifemod.platform.bungee.managers.antialt;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.common.antialt.HeuristicEngine;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import fr.lampalon.lifemod.platform.bungee.utils.MessageUtil;
import net.md_5.bungee.api.connection.PendingConnection;

import java.util.HashMap;
import java.util.Map;
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
            if (result == null) return;
            // Execute reaction based on score
            plugin.getReactionManager().executeReactions(result, connection);
            trackAntiAlt(result);
            if (result.getDangerScore() < 30) {
                IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
                if (ph != null) {
                    ph.capture("lifemod_antialt_pass", new HashMap<>());
                }
            }

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

    private void trackAntiAlt(AnalysisResult result) {
        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            int score = result.getDangerScore();
            Map<String, Object> props = new HashMap<>();
            if (score < 50) props.put("score_range", "30-49");
            else if (score < 70) props.put("score_range", "50-69");
            else if (score < 85) props.put("score_range", "70-84");
            else props.put("score_range", "85-100");
            props.put("severity", score >= 85 ? "suggest_ban" : score >= 70 ? "priority" : score >= 50 ? "alert" : "silent");
            props.put("signals_count", result.getTriggeredRules().size());
            ph.capture("lifemod_antialt", props);
        }
    }
}
