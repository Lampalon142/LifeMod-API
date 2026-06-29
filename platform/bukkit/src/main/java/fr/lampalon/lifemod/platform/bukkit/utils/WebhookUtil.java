package fr.lampalon.lifemod.platform.bukkit.utils;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public final class WebhookUtil {

    private WebhookUtil() {}

    public static void sendAlert(CommandContext context) {
        sendAlert(context, "generic", Map.of());
    }

    public static void sendAlert(CommandContext context, String configKey) {
        sendAlert(context, configKey, Map.of());
    }

    public static void sendAlert(CommandContext context, String configKey, Map<String, String> extraPlaceholders) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;

        String playerName = context.getSender().getName();
        Map<String, String> placeholders = new HashMap<>(extraPlaceholders);
        placeholders.putIfAbsent("%player%", playerName);

        String prefix = "modules.discord.alerts." + configKey;

        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString(prefix + ".title", ""))
                        .setDescription(replace(context.getConfig().getString(prefix + ".description", ""), placeholders))
                        .setFooter(new WebhookFooter(
                                context.getConfig().getString("modules.discord.alerts.footer.text", ""),
                                context.getConfig().getString("modules.discord.alerts.footer.logo", "")
                                        .replace("%player%", playerName)))
                        .setColor(Color.decode(context.getConfig().getString(prefix + ".color", "#FF0000")).getRGB())
                        .build())
                .build()).whenComplete((result, error) -> {
                    if (error != null) {
                        context.getDebug().log("discord", "Webhook error: " + error.getMessage());
                    }
                });
    }

    public static int parseColor(String hex) {
        try {
            return Color.decode(hex).getRGB() & 0xFFFFFF;
        } catch (Exception e) {
            return 0xFF0000;
        }
    }

    private static String replace(String template, Map<String, String> placeholders) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
