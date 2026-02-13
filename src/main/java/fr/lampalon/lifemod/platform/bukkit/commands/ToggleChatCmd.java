package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ToggleChatCmd implements CommandExecutor {

    private final LifeMod plugin = LifeMod.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lifemod.togglechat")) {
            sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("system.no-permission")));
            return true;
        }

        boolean newState = !plugin.isChatEnabled();
        plugin.setChatEnabled(newState);

        String msgKey = newState ? "togglechat.enabled" : "togglechat.disabled";
        sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString(msgKey)));
        return true;
    }
}


