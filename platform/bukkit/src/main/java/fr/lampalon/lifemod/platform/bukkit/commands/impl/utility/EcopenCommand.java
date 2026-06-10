package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;

import java.util.List;
import java.util.Objects;

public class EcopenCommand extends LifeCommand {

    public EcopenCommand() {
        super("ecopen", "lifemod.ecopen", true);
        setDescription("Opens the Ender Chest of another player.");
        setUsage("/ecopen <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.ec.usage"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(context.getArgs()[0]);
        if (targetPlayer == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        if (targetPlayer.equals(context.getPlayer())) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.ec.yourself"));
            return;
        }

        context.getPlayer().openInventory(targetPlayer.getEnderChest());
        context.getDebug().log("ecopen", context.getSender().getName() + " opened ender chest of " + targetPlayer.getName());

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

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
