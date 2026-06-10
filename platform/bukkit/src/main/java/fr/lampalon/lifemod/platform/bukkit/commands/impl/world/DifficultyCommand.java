package fr.lampalon.lifemod.platform.bukkit.commands.impl.world;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;

import java.awt.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DifficultyCommand extends LifeCommand {
    public DifficultyCommand() {
        super("difficulty", "lifemod.difficulty", false);
        setDescription("Changes the server difficulty.");
        setUsage("/difficulty [peaceful|easy|normal|hard]");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length == 0) {
            Difficulty currentDiff = Bukkit.getWorlds().get(0).getDifficulty();
            String available = Arrays.stream(Difficulty.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.joining(", "));
            String msg = context.getLang().getMessage("commands.world.difficulty.current")
                    .replace("%difficulty%", currentDiff.name().toLowerCase())
                    .replace("%available%", available);
            context.getSender().sendMessage(msg);
            return;
        }

        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.difficulty.usage"));
            return;
        }

        String difficultyStr = context.getArgs()[0].toUpperCase();
        try {
            Difficulty difficulty = Difficulty.valueOf(difficultyStr);
            for (World world : Bukkit.getWorlds()) {
                world.setDifficulty(difficulty);
            }
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.difficulty.success", "%difficulty%", difficulty.name().toLowerCase()));

            IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
            if (ph != null) {
                Map<String, Object> props = new HashMap<>();
                props.put("difficulty", difficulty.name().toLowerCase());
                ph.capture("lifemod_difficulty_set", props);
            }

            if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
                sendDiscordAlert(context, difficultyStr);
            }
        } catch (IllegalArgumentException e) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.difficulty.invalid"));
        }
    }

    private void sendDiscordAlert(CommandContext context, String difficulty) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.generic.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.generic.description", "")
                                .replace("%player%", playerName)
                                .replace("%difficulty%", difficulty))
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
            return TabCompleterUtils.filter(Arrays.stream(Difficulty.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.toList()), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
