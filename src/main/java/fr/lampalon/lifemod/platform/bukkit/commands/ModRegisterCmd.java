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
import java.util.UUID;

public class ModRegisterCmd implements CommandExecutor {

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

        if (isRegistered(player.getUniqueId())) {
            player.sendMessage(MessageUtil.formatMessage("&cYou are already registered."));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(MessageUtil.formatMessage("&cUsage: /modregister <password>"));
            return true;
        }

        String password = args[0];

        if (plugin.getConfigConfig().getBoolean("modules.discord.enabled")) {
            // Discord logic...
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        registerModerator(player.getUniqueId(), player.getName(), password, ip);
        player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("commands.auth.registered")));
        return true;
    }

    private boolean isRegistered(java.util.UUID uuid) {
        return LifeMod.getInstance().getModeratorAuthService().isRegistered(uuid);
    }

    private void registerModerator(UUID uuid, String name, String password, String ip) {
        LifeMod.getInstance().getModeratorAuthService().registerModerator(uuid, name, password, ip);
    }
}


