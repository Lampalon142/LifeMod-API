package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LifemodCmd implements CommandExecutor {
    private final LifeMod plugin;

    public LifemodCmd(LifeMod plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("lifemod.admin")) {
                sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("general.nopermission")));
                return true;
            }
            plugin.reloadPluginConfig();
            plugin.reloadLangConfig();
            sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("cmd.reload", "&aConfiguration reloaded!")));
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            for (String line : plugin.getConfigConfig().getStringList("lifemod.info")) {
                sender.sendMessage(MessageUtil.formatMessage(line));
            }
            return true;
        }

        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("cmd.help.header", "&6&lLifeMod &7- Help")));
        sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("cmd.help.reload", "&e/lifemod reload &7- Reload config")));
        sender.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("cmd.help.info", "&e/lifemod info &7- Show info")));
    }
}