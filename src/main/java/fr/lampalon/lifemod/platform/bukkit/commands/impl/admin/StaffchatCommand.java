package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StaffchatCommand extends LifeCommand {
    private final LifeMod plugin;

    public StaffchatCommand(LifeMod plugin) {
        super("staffchat", "lifemod.staffchat", true);
        this.plugin = plugin;
        setDescription("Send messages to staff members only.");
        setUsage("/staffchat <message>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();

        if (args.length == 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.staffchat.usage"));
            return;
        }

        String message = String.join(" ", args);
        String staffchatMessage = context.getLang().getMessage("commands.staffchat.message", "%player%", player.getName()) + ": " + message;

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("lifemod.staffchat")) {
                staff.sendMessage(staffchatMessage);
            }
        }
        context.getSender().sendMessage(context.getLang().getMessage("commands.staffchat.success"));
        context.getDebug().log("staffchat", player.getName() + " sent staffchat message: " + message);

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context, message);
        }
    }

    private void sendDiscordAlert(CommandContext context, String message) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.staffchat.title"))
                    .setDescription(context.getConfig().getString("discord.staffchat.description")
                            .replace("%player%", context.getSender().getName())
                            .replace("%message%", message))
                    .setFooter(context.getConfig().getString("discord.staffchat.footer.title"),
                            context.getConfig().getString("discord.staffchat.footer.logo"))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.staffchat.color")))));
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
            return Collections.singletonList("<message>");
        }
        return super.onTabComplete(context);
    }
}
