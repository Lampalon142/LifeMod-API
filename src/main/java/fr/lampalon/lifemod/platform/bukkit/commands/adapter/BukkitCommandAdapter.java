package fr.lampalon.lifemod.platform.bukkit.commands.adapter;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class BukkitCommandAdapter implements CommandExecutor, TabCompleter {
    private final LifeCommand command;

    public BukkitCommandAdapter(LifeCommand command) {
        this.command = command;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (command.getPermission() != null && !sender.hasPermission(command.getPermission())) {
            sender.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("general.nopermission")));
            return true;
        }

        if (command.isPlayerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("general.onlyplayer")));
            return true;
        }

        command.execute(new BukkitCommandSender(sender), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return command.onTabComplete(new BukkitCommandSender(sender), args);
    }

    private static class BukkitCommandSender implements ICommandSender {
        private final CommandSender sender;

        public BukkitCommandSender(CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public void sendMessage(String message) {
            sender.sendMessage(MessageUtil.formatMessage(message));
        }

        @Override
        public boolean hasPermission(String permission) {
            return sender.hasPermission(permission);
        }

        @Override
        public String getName() {
            return sender.getName();
        }

        @Override
        public UUID getUniqueId() {
            return (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;
        }

        @Override
        public boolean isPlayer() {
            return sender instanceof Player;
        }

        @Override
        public Object getHandle() {
            return sender;
        }
    }
}

