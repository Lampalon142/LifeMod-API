package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.platform.bukkit.utils.BukkitDatabaseUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.util.List;

public class OtpCommand extends LifeCommand {

    public OtpCommand() {
        super("otp", "lifemod.otp", true);
        setDescription("Teleport to an offline player's last known location.");
        setUsage("/otp <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.otp.usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(context.getArgs()[0]);
        if (!target.hasPlayedBefore()) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.otp.player-not-found", "%target%", context.getArgs()[0]));
            return;
        }

        Location location = BukkitDatabaseUtil.fromStoredLocation(context.getPlugin().getDatabaseManager().getDatabaseProvider().getCoords(target.getUniqueId()));
        if (location == null) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.otp.no-position", "%target%", context.getArgs()[0]));
            return;
        }

        context.getPlayer().teleport(location);
        context.getSender().sendMessage(context.getLang().getMessage("commands.otp.teleported", "%target%", context.getArgs()[0]));

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context, context.getArgs()[0]);
        }
    }

    private void sendDiscordAlert(CommandContext context, String targetName) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.generic.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.generic.description", "")
                                .replace("%player%", playerName)
                                .replace("%target%", targetName))
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
