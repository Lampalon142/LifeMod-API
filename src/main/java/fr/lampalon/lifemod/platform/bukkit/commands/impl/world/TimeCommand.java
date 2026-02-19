package fr.lampalon.lifemod.platform.bukkit.commands.impl.world;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TimeCommand extends LifeCommand {
    private final LifeMod plugin;

    public TimeCommand(LifeMod plugin) {
        super("settime", "lifemod.time", false);
        this.plugin = plugin;
        setDescription("Quickly set the world time to day, night, noon, or midnight.");
        setUsage("/settime <day|noon|night|midnight>");
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = context.getArgs();

        if (args.length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.time.usage"));
            return;
        }

        String time = args[0].toLowerCase();
        long ticks;
        switch (time) {
            case "day":
                ticks = 1000;
                break;
            case "noon":
                ticks = 6000;
                break;
            case "night":
                ticks = 13000;
                break;
            case "midnight":
                ticks = 18000;
                break;
            default:
                ticks = -1;
                break;
        }

        if (ticks == -1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.world.time.invalid"));
            return;
        }

        Bukkit.getWorlds().forEach(world -> world.setTime(ticks));
        context.getSender().sendMessage(context.getLang().getMessage("commands.world.time.success", "%time%", time));

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context, time);
        }
    }

    private void sendDiscordAlert(CommandContext context, String time) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.time.title"))
                    .setDescription(context.getConfig().getString("discord.time.description")
                            .replace("%player%", context.getSender().getName())
                            .replace("%time%", time))
                    .setFooter(context.getConfig().getString("discord.time.footer.title"),
                            context.getConfig().getString("discord.time.footer.logo"))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.time.color")))));
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
            return filter(Arrays.asList("day", "noon", "night", "midnight"), context);
        }
        return super.onTabComplete(context);
    }
}
