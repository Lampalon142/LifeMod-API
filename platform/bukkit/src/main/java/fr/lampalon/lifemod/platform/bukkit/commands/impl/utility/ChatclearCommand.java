package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChatclearCommand extends LifeCommand {

    public ChatclearCommand() {
        super("chatclear", "lifemod.chatclear", false);
        setDescription("Clears the chat for all players.");
        setUsage("/chatclear");
    }

    @Override
    public void execute(CommandContext context) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                player.sendMessage("");
            }
            player.sendMessage(context.getLang().getMessage("commands.chatclear.message"));
        }
        context.getDebug().log("chatclear", "Chat cleared by " + context.getSender().getName());

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            ph.capture("lifemod_chat_clear", new HashMap<>());
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.generic.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.generic.description", "")
                                .replace("%player%", playerName))
                        .setFooter(new WebhookFooter(
                                context.getConfig().getString("modules.discord.alerts.footer.text", ""),
                                context.getConfig().getString("modules.discord.alerts.footer.logo", "")
                                        .replace("%player%", playerName)))
                        .setColor(Color.decode(context.getConfig().getString("modules.discord.alerts.generic.color", "#FF0000")).getRGB())
                        .build())
                .build()).whenComplete((result, error) -> {
                    if (error != null) {
                        context.getDebug().log("discord", "Webhook error: " + error.getMessage());
                    }
                });
    }
}
