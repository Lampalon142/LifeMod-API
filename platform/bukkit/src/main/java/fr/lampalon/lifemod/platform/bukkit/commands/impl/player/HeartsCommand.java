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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeartsCommand extends LifeCommand {
    private static final double MAX_HEARTS = 20.0;

    public HeartsCommand() {
        super("hearts", "lifemod.hearts", false);
        setDescription("Manage player hearts: set or add a specific amount of hearts to any player.");
        setUsage("/hearts <set|add> <player> <amount>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 3) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.usage"));
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.unit-info"));
            return;
        }

        String action = context.getArgs()[0].toLowerCase();
        Player target = Bukkit.getPlayer(context.getArgs()[1]);
        if (target == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        double hearts;
        try {
            hearts = Double.parseDouble(context.getArgs()[2]);
        } catch (NumberFormatException e) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.invalid-amount"));
            return;
        }

        hearts = Math.round(hearts * 2.0) / 2.0;

        if (hearts <= 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.invalid-amount"));
            return;
        }

        if (hearts > MAX_HEARTS) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.max-reached"));
            return;
        }

        double healthPoints = hearts * 2.0;
        double newMaxHealth;
        if ("set".equals(action)) {
            newMaxHealth = healthPoints;
        } else if ("add".equals(action)) {
            newMaxHealth = Math.min(target.getMaxHealth() + healthPoints, MAX_HEARTS * 2.0);
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.usage"));
            return;
        }

        newMaxHealth = Math.round(newMaxHealth * 2.0) / 2.0;

        if (newMaxHealth > MAX_HEARTS * 2.0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.max-reached"));
            return;
        }

        if (newMaxHealth % 1 != 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.half-heart-warning"));
        }

        target.setMaxHealth(newMaxHealth);
        target.setHealth(Math.min(target.getHealth(), newMaxHealth));

        context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.success",
                "%action%", action,
                "%target%", target.getName(),
                "%amount%", String.valueOf(hearts)));

        target.sendMessage(context.getLang().getMessage("commands.hearts.changed", "%amount%", String.valueOf(newMaxHealth / 2.0)));

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("target_self", target.equals(context.getSender()));
            ph.capture("lifemod_hearts", props);
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context, target.getName(), action, hearts);
        }
    }

    private void sendDiscordAlert(CommandContext context, String targetName, String action, double amount) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.hearts.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.hearts.description", "")
                                .replace("%player%", playerName)
                                .replace("%target%", targetName)
                                .replace("%action%", action)
                                .replace("%amount%", String.valueOf(amount)))
                        .setFooter(new WebhookFooter(
                                context.getConfig().getString("modules.discord.alerts.footer.text", ""),
                                context.getConfig().getString("modules.discord.alerts.footer.logo", "")
                                        .replace("%player%", playerName)))
                        .setColor(Color.decode(context.getConfig().getString("modules.discord.alerts.hearts.color", "#FF0000")).getRGB())
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
            return TabCompleterUtils.filter(Arrays.asList("set", "add"), context.getArgs()[0]);
        }
        if (context.getArgs().length == 2) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[1]);
        }
        return super.onTabComplete(context);
    }
}
