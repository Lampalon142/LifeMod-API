package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StaffChatEvent implements Listener {
    private final LifeMod plugin;
    private final DebugManager debug;

    public StaffChatEvent(LifeMod plugin) {
        this.plugin = plugin;
        this.debug = plugin.getDebugManager();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        if (!config.getBoolean("modules.staffchat.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("lifemod.staffchat")) return;

        String prefix = config.getString("modules.staffchat.prefix", "!");
        String message = event.getMessage();
        Set<UUID> toggled = plugin.getStaffChatToggled();
        boolean usePrefix = message.startsWith(prefix);
        boolean isToggled = toggled.contains(player.getUniqueId());

        if (!usePrefix && !isToggled) return;

        event.setCancelled(true);
        String rawMsg = usePrefix ? message.substring(prefix.length()) : message;

        ILangService lang = ServiceRegistry.get(ILangService.class);
        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
        String staffMessage = lang.getMessage("commands.staffchat.message",
                "%player%", player.getName(),
                "%message%", rawMsg,
                "%server%", platform.getServerName());

        for (Player recipient : plugin.getServer().getOnlinePlayers()) {
            if (recipient.hasPermission("lifemod.staffchat")) {
                recipient.sendMessage(staffMessage);
            }
        }
        debug.log("staffchat", player.getName() + " sent staffchat message: " + rawMsg);

        IMessagingService messaging = ServiceRegistry.get(IMessagingService.class);
        if (messaging != null) {
            messaging.publish("lifemod:staff", "CHAT|" + player.getUniqueId() + "|" + player.getName() + "|" + rawMsg + "|" + platform.getServerName());
        }

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("word_count", rawMsg.split("\\s+").length);
            props.put("source", usePrefix ? "prefix" : "toggle");
            ph.capture("lifemod_staff_chat", props);
        }

        if (config.getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(player.getName(), rawMsg);
        }
    }

    private void sendDiscordAlert(String playerName, String message) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(config.getString("modules.discord.alerts.generic.title", ""))
                        .setDescription(config.getString("modules.discord.alerts.generic.description", "")
                                .replace("%player%", playerName)
                                .replace("%message%", message))
                        .setFooter(new WebhookFooter(
                                config.getString("modules.discord.alerts.footer.text", ""),
                                config.getString("modules.discord.alerts.footer.logo", "")
                                        .replace("%player%", playerName)))
                        .setColor(Color.decode(config.getString("modules.discord.alerts.generic.color", "#FF0000")).getRGB())
                        .build())
                .build()).whenComplete((result, error) -> {
                    if (error != null) {
                        debug.log("discord", "Webhook error: " + error.getMessage());
                    }
                });
    }
}
