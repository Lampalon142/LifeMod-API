package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.IVanishService;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class VanishCmd implements CommandExecutor {
    private final LifeMod plugin;
    private final DebugManager debug;

    public VanishCmd(LifeMod plugin){
        this.plugin = plugin;
        this.debug = plugin.getDebugManager();
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("vanish")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.player-not-found")));
                return true;
            }

            Player player = (Player) sender;
            IVanishService vanishService = plugin.getVanishService();

            if (player.hasPermission("lifemod.vanish")) {
                if (plugin.getConfigConfig().getBoolean("discord.enabled")){
                    // Discord logic (keep as is)
                }

                if (args.length == 0) {
                    boolean isVanished = vanishService.isVanished(player.getUniqueId());
                    vanishService.setVanished(player, !isVanished, false);
                    if (!isVanished) {
                        player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("vanish.activate")));
                    } else {
                        player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("vanish.deactivate")));
                    }
                } else if (args.length == 1) {
                    Player targetPlayer = Bukkit.getPlayer(args[0]);

                    if (targetPlayer != null) {
                        boolean isVanished = vanishService.isVanished(targetPlayer.getUniqueId());
                        vanishService.setVanished(targetPlayer, !isVanished, false);
                        if (!isVanished) {
                            targetPlayer.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("vanish.activate")));
                            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.vanish-on")));
                        } else {
                            targetPlayer.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("vanish.deactivate")));
                            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.vanish-off")));
                        }
                    } else {
                        player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.player-not-found")));
                    }
                }
            } else {
                player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.no-permission")));
            }
            return true;
        }
        return false;
    }
}


