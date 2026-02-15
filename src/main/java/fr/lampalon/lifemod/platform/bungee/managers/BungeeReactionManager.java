package fr.lampalon.lifemod.platform.bungee.managers;

import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import fr.lampalon.lifemod.platform.bungee.utils.DiscordWebhook;
import fr.lampalon.lifemod.platform.bungee.utils.MessageUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class BungeeReactionManager {

    private final BungeeLifeMod plugin;

    public BungeeReactionManager(BungeeLifeMod plugin) {
        this.plugin = plugin;
    }

    public void executeReactions(AnalysisResult result, PendingConnection connection) {
        int score = result.getDangerScore();
        String playerName = result.getPlayerName();
        String reason = result.getReason();
        String fingerprint = result.getFingerprint();

        // Find matching thresholds in config (Bungee config.yml)
        List<Integer> sortedThresholds = plugin.getConfig().getSection("modules.antialt.thresholds").getKeys()
                .stream()
                .map(Integer::parseInt)
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        for (Integer threshold : sortedThresholds) {
            if (score >= threshold) {
                List<String> actions = plugin.getConfig().getStringList("modules.antialt.thresholds." + threshold + ".actions");
                for (String actionScript : actions) {
                    executeScript(actionScript, playerName, score, reason, fingerprint, connection);
                }
            }
        }
    }

    private void executeScript(String script, String playerName, int score, String reason, String fingerprint, PendingConnection connection) {
        String upper = script.toUpperCase();

        // Replace placeholders
        script = script.replace("%player%", playerName)
                       .replace("%score%", String.valueOf(score))
                       .replace("%reason%", reason)
                       .replace("%fingerprint%", fingerprint);

        if (upper.startsWith("[CONSOLE]")) {
            String cmd = script.substring(9).trim();
            plugin.getProxy().getScheduler().runAsync(plugin, () -> { // Ensure execution is async as it might be blocking
                plugin.getProxy().getPluginManager().dispatchCommand(plugin.getProxy().getConsole(), cmd);
            });
        }
        else if (upper.startsWith("[MESSAGE]")) {
            String message = fr.lampalon.lifemod.platform.bungee.utils.MessageUtil.formatMessage(script.substring(9).trim());
            // If player is still connecting, kick them immediately with the message
            if (connection != null && !connection.isOnline()) {
                connection.disconnect(new TextComponent(message));
            } else {
                // Otherwise, publish to Redis for target server to send the message
                plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                    if (ServiceRegistry.get(fr.lampalon.lifemod.common.messaging.IMessagingService.class) != null) {
                        ServiceRegistry.get(fr.lampalon.lifemod.common.messaging.IMessagingService.class).publish("lifemod:antialt_actions", "MESSAGE|" + connection.getUniqueId() + "|" + message);
                    }
                });
            }
        }
        else if (upper.startsWith("[STAFF]")) { // Notify staff (could be on other servers)
            String message = fr.lampalon.lifemod.platform.bungee.utils.MessageUtil.formatMessage(script.substring(7).trim());
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                // Send to local staff
                for (net.md_5.bungee.api.connection.ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                    if (p.hasPermission("lifemod.antialt.notify")) {
                        p.sendMessage(new TextComponent(message));
                    }
                }
                // Publish to Redis for staff on Bukkit servers
                if (ServiceRegistry.get(fr.lampalon.lifemod.common.messaging.IMessagingService.class) != null) {
                    ServiceRegistry.get(fr.lampalon.lifemod.common.messaging.IMessagingService.class).publish("lifemod:antialt_actions", "STAFF_ALERT|" + message);
                }
            });
        }
        else if (upper.startsWith("[LOG]")) {
            String message = script.substring(5).trim();
            plugin.getLogger().info(message);
        }
        else if (upper.startsWith("[DISCORD]")) {
            String discordMessage = script.substring(9).trim();
            if (plugin.getConfig().getBoolean("modules.discord.enabled")) {
                plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                    try {
                        DiscordWebhook webhook = new DiscordWebhook(plugin.getConfig().getString("modules.discord.webhook-url"));
                        webhook.addEmbed(new DiscordWebhook.EmbedObject()
                                .setTitle("AntiAlt Alert Bungee")
                                .setDescription(discordMessage)
                                .setColor(Color.RED));
                        webhook.execute();
                    } catch (IOException e) {
                        plugin.getLogger().warning("Webhook error for AntiAlt Bungee: " + e.getMessage());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Unknown error sending Discord webhook for AntiAlt Bungee: " + e.getMessage());
                    }
                });
            }
        }
        else if (upper.startsWith("[KICK]")) {
            String message = script.substring(6).trim();
            if (connection != null && !connection.isOnline()) { // Only if player is connecting
                connection.disconnect(new TextComponent(message));
            } else {
                // Publish to Redis for target server to kick the player
                plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                    if (ServiceRegistry.get(fr.lampalon.lifemod.common.messaging.IMessagingService.class) != null) {
                        ServiceRegistry.get(fr.lampalon.lifemod.common.messaging.IMessagingService.class).publish("lifemod:antialt_actions", "KICK|" + connection.getUniqueId() + "|" + message);
                    }
                });
            }
        }
    }
}
