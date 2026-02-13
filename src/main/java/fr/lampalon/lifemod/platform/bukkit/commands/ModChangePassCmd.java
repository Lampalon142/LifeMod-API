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

public class ModChangePassCmd implements CommandExecutor {

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

        if (args.length != 2) {
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("commands.auth.changepass-usage")));
            return true;
        }

        String oldPass = args[0];
        String newPass = args[1];

        if (!checkPassword(player.getUniqueId(), oldPass)) {
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("commands.auth.incorrect-old-password")));
            return true;
        }

        if (plugin.getConfigConfig().getBoolean("modules.discord.enabled")) {
            // Discord logic...
        }

        changePassword(player.getUniqueId(), newPass);
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("commands.auth.password-changed")));
        return true;
    }

    private boolean checkPassword(java.util.UUID uuid, String password) {
        return LifeMod.getInstance().getModeratorAuthService().checkPassword(uuid, password);
    }

    private void changePassword(java.util.UUID uuid, String newPassword) {
        LifeMod.getInstance().getModeratorAuthService().changePassword(uuid, newPassword);
    }
}


