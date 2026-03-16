package fr.lampalon.lifemod.platform.bukkit.commands.impl.world;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DifficultyCommand extends LifeCommand {
    private final LifeMod plugin;

    public DifficultyCommand(LifeMod plugin) {
        super("difficulty", "lifemod.difficulty", false);
        this.plugin = plugin;
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
            if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
                sendDiscordAlert(context, difficultyStr);
            }
        } catch (IllegalArgumentException e) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.difficulty.invalid"));
        }
    }

    private void sendDiscordAlert(CommandContext context, String difficulty) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.difficulty.title", ""))
                    .setDescription(context.getConfig().getString("discord.difficulty.description", "")
                            .replace("%player%", context.getSender().getName())
                            .replace("%difficulty%", difficulty))
                    .setFooter(context.getConfig().getString("discord.difficulty.footer.title", ""),
                            context.getConfig().getString("discord.difficulty.footer.logo", ""))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.difficulty.color", "")))));
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
            return TabCompleterUtils.filter(Arrays.stream(Difficulty.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.toList()), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
