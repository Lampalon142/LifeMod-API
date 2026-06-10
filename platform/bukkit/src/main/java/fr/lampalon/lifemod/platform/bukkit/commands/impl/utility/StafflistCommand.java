package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;

import java.util.Objects;

public class StafflistCommand extends LifeCommand {

    public StafflistCommand() {
        super("stafflist", "lifemod.stafflist", true);
        setDescription("View currently online staff members.");
        setUsage("/stafflist");
    }

    @Override
    public void execute(CommandContext context) {
        StringBuilder modList = new StringBuilder(context.getLang().getMessage("commands.stafflist.online"));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("lifemod.stafflist") && !context.getPlugin().getVanishService().isVanished(onlinePlayer.getUniqueId())) {
                modList.append(onlinePlayer.getName()).append(", ");
            }
        }

        if (modList.length() > context.getLang().getMessage("commands.stafflist.online").length()) {
            modList.delete(modList.length() - 2, modList.length());
        } else {
            modList.append(context.getLang().getMessage("commands.stafflist.none"));
        }

        context.getSender().sendMessage(modList.toString());

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
