package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil; // Keep MessageUtil for parseColors

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BroadcastCommand extends LifeCommand {
    private final LifeMod plugin;

    public BroadcastCommand(LifeMod plugin) {
        super("broadcast", "lifemod.bc", false, "bc");
        this.plugin = plugin;
        setDescription("Broadcasts a message to the entire server.");
        setUsage("/broadcast <message>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.broadcast.usage"));
            return;
        }

        String message = String.join(" ", context.getArgs()).replace(" ", " ");
        String prefix = context.getLang().getMessage("commands.broadcast.prefix");
        String broadcast = MessageUtil.parseColors(prefix + message); // Still using MessageUtil for color parsing

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(broadcast);
        }
        Bukkit.getConsoleSender().sendMessage(broadcast);

        context.getDebug().log("broadcast", "Broadcast sent by " + context.getSender().getName() + ": " + message);

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context.getSender().getName(), message, context);
        }
    }

    private void sendDiscordAlert(String playerName, String message, CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.broadcast.title", ""))
                    .setDescription(context.getConfig().getString("discord.broadcast.description", "")
                            .replace("%player%", playerName)
                            .replace("%message%", message))
                    .setFooter(
                            context.getConfig().getString("discord.broadcast.footer.title", ""),
                            context.getConfig().getString("discord.broadcast.footer.logo", "")
                                    .replace("%player%", playerName)
                    )
                    .setColor(Color.decode(Objects.requireNonNull(
                            context.getConfig().getString("discord.broadcast.color", "")
                    ))));
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
            // Assuming "bc.tabcompleter" is a list of default suggestions for the message
            List<String> suggestions = context.getPlugin().getLangConfig().getStringList("bc.tabcompleter"); // Still direct config access
            return TabCompleterUtils.filter(suggestions, context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
