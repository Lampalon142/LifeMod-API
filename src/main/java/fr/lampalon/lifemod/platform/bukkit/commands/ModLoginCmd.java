package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class ModLoginCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LifeMod plugin = LifeMod.getInstance();
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.player-only")));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("lifemod.moderator")) {
            sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.no-permission")));
            return true;
        }

        if (isAuthenticated(player)) {
            player.sendMessage(MessageUtil.formatMessage("&cYou are already authenticated."));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(MessageUtil.formatMessage("&cUsage: /modlogin <password>"));
            return true;
        }

        String password = args[0];

        if (plugin.getConfigConfig().getBoolean("modules.discord.enabled")) {
            // Discord logic...
        }

        if (checkPassword(player.getUniqueId(), password)) {
            authenticate(player);
            player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("commands.auth.login-success")));
        } else {
            int attemptsLeft = decrementAttempts(player);
            if (attemptsLeft <= 0) {
                lockModerator(player);
                player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("commands.auth.login-locked")));
            } else {
                String rawMsg = plugin.getLangConfig().getString("commands.auth.login-failed", "&cIncorrect password! Attempts left: &e%attempts%");
                String msg = rawMsg.replace("%attempts%", String.valueOf(attemptsLeft));
                player.sendMessage(MessageUtil.formatMessage(msg));
            }
        }
        return true;
    }

    private boolean isAuthenticated(Player player) {
        return LifeMod.getInstance().getModeratorSessionManager().isAuthenticated(player.getUniqueId());
    }

    private boolean checkPassword(java.util.UUID uuid, String password) {
        return LifeMod.getInstance().getModeratorAuthService().checkPassword(uuid, password);
    }

    private void authenticate(Player player) {
        LifeMod.getInstance().getModeratorSessionManager().authenticate(player.getUniqueId());
    }

    private int decrementAttempts(Player player) {
        return LifeMod.getInstance().getModeratorSessionManager().decrementAttempts(player.getUniqueId());
    }

    private void lockModerator(Player player) {
        LifeMod.getInstance().getModeratorSessionManager().lock(player.getUniqueId());
    }
}


