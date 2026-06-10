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
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeedCommand extends LifeCommand {
    public SpeedCommand() {
        super("speed", "speed.use", true);
        setDescription("Sets the walk or fly speed of the player.");
        setUsage("/speed <1-10>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length == 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.speed.provide"));
            return;
        }

        int speed;
        try {
            speed = Integer.parseInt(context.getArgs()[0]);
        } catch (NumberFormatException e) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.speed.provide"));
            return;
        }

        if (speed < 1 || speed > 10) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.speed.provide"));
            return;
        }

        if (player.isFlying()) {
            player.setFlySpeed((float) speed / 10);
        } else {
            player.setWalkSpeed((float) speed / 10);
        }

        context.getSender().sendMessage(context.getLang().getMessage("commands.speed.success", "%speed%", String.valueOf(speed)));
        context.getDebug().log("speed", player.getName() + " changed speed to " + speed);

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("speed_type", player.isFlying() ? "fly" : "walk");
            props.put("speed_value", speed);
            ph.capture("lifemod_speed", props);
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
            return TabCompleterUtils.filter(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
