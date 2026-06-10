package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealCommand extends LifeCommand {
    public HealCommand() {
        super("heal", "lifemod.heal", true);
        setDescription("Heals a player, restoring their health and hunger.");
        setUsage("/heal [player]");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length == 0) {
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.heal.player"));
            context.getDebug().log("heal", player.getName() + " healed himself");
        } else if (context.getArgs().length == 1) {
            Player target = Bukkit.getPlayer(context.getArgs()[0]);
            if (target != null) {
                target.setHealth(target.getMaxHealth());
                target.setFoodLevel(20);
                context.getSender().sendMessage(context.getLang().getMessage("commands.utility.heal.mod", "%player%", target.getName()));
                target.sendMessage(context.getLang().getMessage("commands.utility.heal.player"));
                context.getDebug().log("heal", player.getName() + " healed " + target.getName());
            } else {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            }
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.heal.usage"));
        }

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("target_self", context.getArgs().length == 0);
            ph.capture("lifemod_heal", props);
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
