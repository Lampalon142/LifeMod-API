package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ReactionManager {

    private final LifeMod plugin;
    private final DebugManager debug;

    public ReactionManager(LifeMod plugin) {
        this.plugin = plugin;
        this.debug = plugin.getDebugManager();
    }

    public void executeReactions(AnalysisResult result, Player targetPlayer) {
        int score = result.getDangerScore();
        String playerName = result.getPlayerName();
        String reason = result.getReason();
        String fingerprint = result.getFingerprint();
        
        // Find matching thresholds in config
        List<Integer> sortedThresholds = plugin.getConfigConfig().getConfigurationSection("modules.antialt.thresholds").getKeys(false)
                .stream()
                .map(Integer::parseInt)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        
        List<String> reactionsToExecute = new java.util.ArrayList<>();
        for (Integer threshold : sortedThresholds) {
            if (score >= threshold) {
                List<String> actions = plugin.getConfigConfig().getStringList("modules.antialt.thresholds." + threshold);
                reactionsToExecute.addAll(actions);
            }
        }
        
        for (String reactionScript : reactionsToExecute) {
            executeScript(reactionScript, playerName, score, reason, fingerprint, targetPlayer);
        }
    }

    private void executeScript(String script, String playerName, int score, String reason, String fingerprint, Player targetPlayer) {
        String upper = script.toUpperCase();
        
        // Replace placeholders
        script = script.replace("%player%", playerName)
                       .replace("%score%", String.valueOf(score))
                       .replace("%reason%", reason)
                       .replace("%fingerprint%", fingerprint);
        
        if (upper.startsWith("[CONSOLE]")) {
            String cmd = script.substring(9).trim();
            Bukkit.getScheduler().runTask(plugin, () -> { // Ensure command execution is on main thread
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            });
        }
        else if (upper.startsWith("[PLAYER]")) { // Should not be used for Antialt, but for consistency
            String cmd = script.substring(8).trim();
            if (targetPlayer != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    targetPlayer.performCommand(cmd);
                });
            } else {
                debug.log("antialt", "Cannot execute [PLAYER] command for " + playerName + " (not online).");
            }
        }
        else if (upper.startsWith("[MESSAGE]")) {
            String message = MessageUtil.formatMessage(script.substring(9).trim());
            if (targetPlayer != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    targetPlayer.sendMessage(message);
                });
            }
        }
        else if (upper.startsWith("[STAFF]")) {
            String message = MessageUtil.formatMessage(script.substring(7).trim());
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("lifemod.antialt.notify"))
                        .forEach(p -> p.sendMessage(message));
            });
        }
        else if (upper.startsWith("[LOG]")) {
            String message = script.substring(5).trim();
            plugin.getLogger().info(message);
        }
        else if (upper.startsWith("[DISCORD]")) {
            String discordMessage = script.substring(9).trim();
            if (plugin.getConfigConfig().getBoolean("modules.discord.enabled")) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        DiscordWebhook webhook = new DiscordWebhook(plugin.webHookUrl);
                        webhook.addEmbed(new DiscordWebhook.EmbedObject()
                                .setTitle("AntiAlt Alert")
                                .setDescription(discordMessage)
                                .setColor(Color.RED)); // Or a configurable color
                        webhook.execute();
                    } catch (IOException e) {
                        debug.log("discord", "Webhook error for AntiAlt: " + e.getMessage());
                    } catch (Exception e) {
                        debug.log("discord", "Unknown error sending Discord webhook for AntiAlt: " + e.getMessage());
                    }
                });
            }
        }
        else if (upper.startsWith("[ACTIONBAR]")) {
            String message = MessageUtil.formatMessage(script.substring(11).trim());
            if (targetPlayer != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getPacketController().sendActionBar(targetPlayer, message);
                });
            }
        }
        // ... any other tags as needed
    }
}
