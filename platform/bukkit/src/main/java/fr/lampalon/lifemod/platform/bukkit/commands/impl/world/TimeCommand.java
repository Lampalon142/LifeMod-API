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
import org.bukkit.Bukkit;

import java.awt.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TimeCommand extends LifeCommand {

    public TimeCommand() {
        super("settime", "lifemod.time", false);
        setDescription("Quickly set the world time to day, night, noon, or midnight.");
        setUsage("/settime <day|noon|night|midnight>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.time.usage"));
            return;
        }

        String time = context.getArgs()[0].toLowerCase();
        long ticks;
        switch (time) {
            case "day": ticks = 1000; break;
            case "noon": ticks = 6000; break;
            case "night": ticks = 13000; break;
            case "midnight": ticks = 18000; break;
            default: ticks = -1; break;
        }

        if (ticks == -1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.time.invalid"));
            return;
        }

        Bukkit.getWorlds().forEach(world -> world.setTime(ticks));
        context.getSender().sendMessage(context.getLang().getMessage("commands.world.time.success", "%time%", time));

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("time_value", time);
            ph.capture("lifemod_time_set", props);
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context, time);
        }
    }

    private void sendDiscordAlert(CommandContext context, String time) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.generic.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.generic.description", "")
                                .replace("%player%", playerName)
                                .replace("%time%", time))
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
            return TabCompleterUtils.filter(Arrays.asList("day", "noon", "night", "midnight"), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
