package fr.lampalon.lifemod.platform.bukkit.commands.impl.world;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;

import java.util.Arrays;
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

            if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
                WebhookUtil.sendAlert(context, "generic", Map.of("%difficulty%", difficultyStr));
            }
        } catch (IllegalArgumentException e) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.difficulty.invalid"));
        }
    }



    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filter(Arrays.stream(Difficulty.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.toList()), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
