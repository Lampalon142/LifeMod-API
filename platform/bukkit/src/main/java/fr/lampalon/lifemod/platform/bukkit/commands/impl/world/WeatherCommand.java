package fr.lampalon.lifemod.platform.bukkit.commands.impl.world;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.awt.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WeatherCommand extends LifeCommand {

    public WeatherCommand() {
        super("weather", "lifemod.weather", true);
        setDescription("Changes the weather in the current world.");
        setUsage("/weather <clear|rain|storm>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        World world = player.getWorld();

        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.weather.usage"));
            return;
        }

        String weatherType = context.getArgs()[0].toLowerCase();

        switch (weatherType) {
            case "clear":
            case "sun":
                world.setStorm(false);
                world.setThundering(false);
                context.getSender().sendMessage(context.getLang().getMessage("commands.world.weather.sun"));
                break;
            case "rain":
                world.setStorm(true);
                world.setThundering(false);
                context.getSender().sendMessage(context.getLang().getMessage("commands.world.weather.rain"));
                break;
            case "storm":
                world.setStorm(true);
                world.setThundering(true);
                context.getSender().sendMessage(context.getLang().getMessage("commands.world.weather.storm"));
                break;
            default:
                context.getSender().sendMessage(context.getLang().getMessage("commands.world.weather.usage"));
                return;
        }

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("weather_type", weatherType);
            ph.capture("lifemod_weather_set", props);
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context, weatherType);
        }
    }

    private void sendDiscordAlert(CommandContext context, String weatherType) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.generic.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.generic.description", "")
                                .replace("%player%", playerName)
                                .replace("%weather%", weatherType))
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
            return TabCompleterUtils.filter(Arrays.asList("clear", "sun", "rain", "storm"), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
