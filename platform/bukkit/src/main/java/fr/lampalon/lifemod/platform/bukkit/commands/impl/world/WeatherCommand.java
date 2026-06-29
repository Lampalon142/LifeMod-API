package fr.lampalon.lifemod.platform.bukkit.commands.impl.world;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Arrays;
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

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context, "generic", Map.of("%weather%", weatherType));
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
