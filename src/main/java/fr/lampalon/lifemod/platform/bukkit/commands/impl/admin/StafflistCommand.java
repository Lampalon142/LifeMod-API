package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class StafflistCommand extends LifeCommand {
    private final LifeMod plugin;

    public StafflistCommand(LifeMod plugin) {
        super("stafflist", "lifemod.stafflist", true);
        this.plugin = plugin;
        setDescription("View currently online staff members.");
        setUsage("/stafflist");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        StringBuilder modList = new StringBuilder(context.getLang().getMessage("commands.stafflist.online"));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // Check for lifemod.stafflist permission and not vanished
            if (onlinePlayer.hasPermission("lifemod.stafflist") && !plugin.getVanishService().isVanished(onlinePlayer.getUniqueId())) {
                modList.append(onlinePlayer.getName()).append(", ");
            }
        }

        if (modList.length() > context.getLang().getMessage("commands.stafflist.online").length()) { // Check if any staff were added
            modList.delete(modList.length() - 2, modList.length()); // Remove trailing ", "
        } else {
            modList.append(context.getLang().getMessage("commands.stafflist.none"));
        }

        player.sendMessage(modList.toString());

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.stafflist.title"))
                    .setDescription(context.getConfig().getString("discord.stafflist.description")
                            .replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("discord.stafflist.footer.title"),
                            context.getConfig().getString("discord.stafflist.footer.logo"))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.stafflist.color")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
