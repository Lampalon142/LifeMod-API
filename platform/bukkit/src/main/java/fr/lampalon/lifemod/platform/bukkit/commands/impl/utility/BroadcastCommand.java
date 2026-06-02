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

        String message = String.join(" ", context.getArgs());
        String formatted = context.getLang().getMessage("commands.broadcast.format", "%message%", message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage(formatted);

        context.getDebug().log("broadcast", "Broadcast sent by " + context.getSender().getName() + ": " + message);

        if (context.getPlugin().getConfigConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context.getSender().getName(), message, context);
        }
    }

    private void sendDiscordAlert(String playerName, String message, CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("modules.discord.broadcast.title", ""))
                    .setDescription(context.getConfig().getString("modules.discord.broadcast.description", "")
                            .replace("%player%", playerName)
                            .replace("%message%", message))
                    .setFooter(
                            context.getConfig().getString("modules.discord.broadcast.footer.title", ""),
                            context.getConfig().getString("modules.discord.broadcast.footer.logo", "")
                                    .replace("%player%", playerName)
                    )
                    .setColor(Color.decode(Objects.requireNonNull(
                            context.getConfig().getString("modules.discord.broadcast.color", "#60a5fa")
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
            List<String> suggestions = context.getLang().getStringList("commands.broadcast.tab-completer");
            return TabCompleterUtils.filter(suggestions, context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
