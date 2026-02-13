package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.StaffModeManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class ModCmd implements CommandExecutor {
    private final LifeMod plugin;
    private final StaffModeManager staffModeManager;
    private final DebugManager debug;

    public ModCmd(LifeMod plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
        this.debug = plugin.getDebugManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.player-only")));
            debug.log("mod", "Console tried to use /mod");
            return false;
        }

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("mod") || label.equalsIgnoreCase("staff")) {

            if (!player.hasPermission("lifemod.mod")) {
                player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.no-permission")));
                debug.log("mod", "Permission denied for /mod by " + player.getName());
                return false;
            }

            // Discord Webhook Logic (Preserved but could be moved to manager event later)
            if (plugin.getConfigConfig().getBoolean("discord.enabled")) {
                try {
                    DiscordWebhook webhook = new DiscordWebhook(plugin.webHookUrl);
                    webhook.addEmbed(new DiscordWebhook.EmbedObject()
                            .setTitle(plugin.getConfigConfig().getString("discord.mod.title"))
                            .setDescription(plugin.getConfigConfig().getString("discord.mod.description").replace("%player%", sender.getName()))
                            .setFooter(plugin.getConfigConfig().getString("discord.mod.footer.title"),
                                    plugin.getConfigConfig().getString("discord.mod.footer.logo").replace("%player%", sender.getName()))
                            .setColor(Color.decode(Objects.requireNonNull(plugin.getConfigConfig().getString("discord.mod.color")))));
                    webhook.execute();
                } catch (Exception e) {
                    debug.log("discord", "Webhook error: " + e.getMessage());
                }
            }

            if (staffModeManager.isMod(player)) {
                staffModeManager.disableStaffMode(player);
            } else {
                staffModeManager.enableStaffMode(player);
            }
        }
        return false;
    }
}


