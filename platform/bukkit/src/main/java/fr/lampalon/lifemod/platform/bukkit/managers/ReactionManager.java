package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class ReactionManager {

    private final LifeMod plugin;

    public ReactionManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    public void executeReactions(AnalysisResult result, Player targetPlayer) {
        int score = result.getDangerScore();
        String playerName = result.getPlayerName();
        
        // Log & Debug
        if (plugin.getConfigConfig().getBoolean("modules.antialt.debug")) {
            plugin.getLogger().info("[AntiAlt Debug] Player: " + playerName + " | Score: " + score + " | Reason: " + result.getReason());
        }

        // Sync via Redis for the whole network
        IMessagingService redis = ServiceRegistry.get(IMessagingService.class);
        if (redis != null && score >= 30) {
            redis.publish("lifemod:antialt_sync", "SUSPICION|" + playerName + "|" + score + "|" + result.getFingerprint());
        }

        // Process Thresholds
        plugin.getConfigConfig().getConfigurationSection("modules.antialt.thresholds").getKeys(false).stream()
                .map(Integer::parseInt)
                .filter(t -> score >= t)
                .sorted((a, b) -> b - a) // Process higher thresholds first
                .findFirst()
                .ifPresent(threshold -> {
                    List<String> actions = plugin.getConfigConfig().getStringList("modules.antialt.thresholds." + threshold);
                    for (String action : actions) {
                        executeScript(action, result, targetPlayer);
                    }
                });
    }

    private void executeScript(String script, AnalysisResult result, Player targetPlayer) {
        String formatted = script.replace("%player%", result.getPlayerName())
                                .replace("%score%", String.valueOf(result.getDangerScore()))
                                .replace("%reason%", result.getReason())
                                .replace("%fingerprint%", result.getFingerprint());

        String upper = formatted.toUpperCase();

        if (upper.startsWith("[CONSOLE]")) {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted.substring(9).trim()));
        } else if (upper.startsWith("[STAFF]")) {
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            String msg = lang.formatMessage(formatted.substring(7).trim());
            Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("lifemod.antialt.notify")).forEach(p -> p.sendMessage(msg));
        } else if (upper.startsWith("[CANCEL]")) {
            if (targetPlayer != null) {
                fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                Bukkit.getScheduler().runTask(plugin, () -> targetPlayer.kickPlayer(lang.formatMessage(formatted.substring(8).trim())));
            }
        } else if (upper.startsWith("[LOG]")) {
            plugin.getLogger().warning("[AntiAlt] " + formatted.substring(5).trim());
        }
    }
}
