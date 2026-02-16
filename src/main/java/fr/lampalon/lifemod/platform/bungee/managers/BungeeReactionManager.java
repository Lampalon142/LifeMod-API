package fr.lampalon.lifemod.platform.bungee.managers;

import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import fr.lampalon.lifemod.platform.bungee.utils.MessageUtil;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;

import java.util.List;

public class BungeeReactionManager {

    private final BungeeLifeMod plugin;

    public BungeeReactionManager(BungeeLifeMod plugin) {
        this.plugin = plugin;
    }

    public void executeReactions(AnalysisResult result, PendingConnection connection) {
        int score = result.getDangerScore();
        
        // Sync via Redis
        IMessagingService redis = ServiceRegistry.get(IMessagingService.class);
        if (redis != null && score >= 30) {
            redis.publish("lifemod:antialt_sync", "SUSPICION|" + result.getPlayerName() + "|" + score + "|" + result.getFingerprint());
        }

        // Process Thresholds (Bungee Configuration)
        plugin.getConfig().getSection("modules.antialt.thresholds").getKeys().stream()
                .map(Integer::parseInt)
                .filter(t -> score >= t)
                .sorted((a, b) -> b - a)
                .findFirst()
                .ifPresent(threshold -> {
                    List<String> actions = plugin.getConfig().getStringList("modules.antialt.thresholds." + threshold + ".actions");
                    for (String action : actions) {
                        executeScript(action, result, connection);
                    }
                });
    }

    private void executeScript(String script, AnalysisResult result, PendingConnection connection) {
        fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        String rules = result.getTriggeredRules().stream()
                .map(r -> lang.getMessage("antialt.rules." + r.getKey()))
                .collect(java.util.stream.Collectors.joining(", "));

        String formatted = script.replace("%player%", result.getPlayerName())
                                .replace("%score%", String.valueOf(result.getDangerScore()))
                                .replace("%reason%", rules)
                                .replace("%fingerprint%", result.getFingerprint());

        String upper = formatted.toUpperCase();

        if (upper.startsWith("[CONSOLE]")) {
            plugin.getProxy().getPluginManager().dispatchCommand(plugin.getProxy().getConsole(), formatted.substring(9).trim());
        } else if (upper.startsWith("[STAFF]")) {
            TextComponent msg = new TextComponent(MessageUtil.formatMessage(formatted.substring(7).trim()));
            plugin.getProxy().getPlayers().stream().filter(p -> p.hasPermission("lifemod.antialt.notify")).forEach(p -> p.sendMessage(msg));
        } else if (upper.startsWith("[CANCEL]") || upper.startsWith("[KICK]")) {
            String reason = upper.startsWith("[CANCEL]") ? formatted.substring(8).trim() : formatted.substring(6).trim();
            connection.disconnect(new TextComponent(MessageUtil.formatMessage(reason)));
        } else if (upper.startsWith("[LOG]")) {
            plugin.getLogger().warning("[AntiAlt-Bungee] " + formatted.substring(5).trim());
        }
    }
}
