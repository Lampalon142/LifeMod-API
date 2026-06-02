package fr.lampalon.lifemod.platform.bukkit.commands.impl.world;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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

        if (context.getPlugin().getConfigConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context, weatherType);
        }
    }

    private void sendDiscordAlert(CommandContext context, String weatherType) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.weather.title", ""))
                    .setDescription(context.getConfig().getString("discord.weather.description", "")
                            .replace("%player%", context.getSender().getName())
                            .replace("%weather%", weatherType))
                    .setFooter(context.getConfig().getString("discord.weather.footer.title", ""),
                            context.getConfig().getString("discord.weather.footer.logo", ""))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.weather.color", "")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filter(Arrays.asList("clear", "sun", "rain", "storm"), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
