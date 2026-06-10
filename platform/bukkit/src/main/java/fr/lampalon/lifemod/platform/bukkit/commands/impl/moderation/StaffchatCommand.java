package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class StaffchatCommand extends LifeCommand {

    public StaffchatCommand() {
        super("staffchat", "lifemod.staffchat", true);
        setDescription("Send messages to staff members only.");
        setUsage("/staffchat <message>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length == 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.staffchat.usage"));
            return;
        }

        String message = String.join(" ", context.getArgs());
        String staffchatMessage = context.getLang().getMessage("commands.staffchat.message", "%player%", context.getPlayer().getName()) + ": " + message;

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("lifemod.staffchat")) {
                staff.sendMessage(staffchatMessage);
            }
        }
        context.getSender().sendMessage(context.getLang().getMessage("commands.staffchat.success"));
        context.getDebug().log("staffchat", context.getSender().getName() + " sent staffchat message: " + message);

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
            return Collections.singletonList("<message>");
        }
        return super.onTabComplete(context);
    }
}
