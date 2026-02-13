package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class ModResetCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LifeMod plugin = LifeMod.getInstance();
        if (!sender.hasPermission("lifemod.admin")) {
            sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.no-permission")));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("commands.auth.reset-usage")));
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = getUUIDByName(targetName);

        if (targetUUID == null || !isRegistered(targetUUID)) {
            sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.player-not-found")));
            return true;
        }

        if (plugin.getConfigConfig().getBoolean("modules.discord.enabled")) {
            // Discord logic...
        }

        resetModeratorPassword(targetUUID);

        String rawMsg = plugin.getLangConfig().getString("commands.auth.reset-success");
        String msg = rawMsg.replace("%player%", targetName);
        sender.sendMessage(MessageUtil.formatMessage(msg));
        return true;
    }

    private boolean isRegistered(UUID uuid) {
        return LifeMod.getInstance().getModeratorAuthService().isRegistered(uuid);
    }

    private void resetModeratorPassword(UUID uuid) {
        LifeMod.getInstance().getModeratorAuthService().resetModeratorPassword(uuid);
    }

    private UUID getUUIDByName(String name) {
        return LifeMod.getInstance().getModeratorAuthService().getUUIDByName(name);
    }
}


