package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedCommand extends LifeCommand {

    public FeedCommand() {
        super("feed", "lifemod.feed", true);
        setDescription("Feeds a player, restoring their hunger.");
        setUsage("/feed [player]");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length == 0) {
            context.getPlayer().setFoodLevel(20);
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.feed.yourself"));
        } else if (context.getArgs().length == 1) {
            Player target = Bukkit.getPlayer(context.getArgs()[0]);
            if (target == null) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }
            target.setFoodLevel(20);
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.feed.mod", "%target%", target.getName()));
            target.sendMessage(context.getLang().getMessage("commands.utility.feed.player"));
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.feed.usage"));
        }

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("target_self", context.getArgs().length == 0);
            ph.capture("lifemod_feed", props);
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

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
