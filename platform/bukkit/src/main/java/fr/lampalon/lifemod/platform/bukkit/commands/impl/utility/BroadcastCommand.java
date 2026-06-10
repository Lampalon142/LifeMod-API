package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil; // Keep MessageUtil for parseColors

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BroadcastCommand extends LifeCommand {
    public BroadcastCommand() {
        super("broadcast", "lifemod.bc", false, "bc");
        setDescription("Broadcasts a message to the entire server.");
        setUsage("/broadcast <message>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.broadcast.usage"));
            return;
        }

        String message = String.join(" ", context.getArgs());
        String formatted = context.getLang().getMessage("commands.broadcast.format", "%message%", message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage(formatted);

        context.getDebug().log("broadcast", "Broadcast sent by " + context.getSender().getName() + ": " + message);

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("has_message", true);
            ph.capture("lifemod_broadcast", props);
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context, message);
        }
    }

    private void sendDiscordAlert(CommandContext context, String message) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.generic.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.generic.description", "")
                                .replace("%player%", playerName)
                                .replace("%message%", message))
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

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            List<String> suggestions = context.getLang().getStringList("commands.broadcast.tab-completer");
            return TabCompleterUtils.filter(suggestions, context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
