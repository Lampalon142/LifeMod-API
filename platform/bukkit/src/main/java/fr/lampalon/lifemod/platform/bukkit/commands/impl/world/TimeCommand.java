package fr.lampalon.lifemod.platform.bukkit.commands.impl.world;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;

import java.util.Arrays;
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

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context, "generic", Map.of("%time%", time));
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filter(Arrays.asList("day", "noon", "night", "midnight"), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
