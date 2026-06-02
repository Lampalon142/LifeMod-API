package fr.lampalon.lifemod.platform.bukkit.commands.engine;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class BukkitCommandAdapter implements CommandExecutor, TabCompleter {
    private final LifeCommand lifeCommand;
    private final LifeMod plugin;
    private final fr.lampalon.lifemod.common.service.ILangService lang;

    public BukkitCommandAdapter(LifeCommand lifeCommand, LifeMod plugin) {
        this.lifeCommand = lifeCommand;
        this.plugin = plugin;
        this.lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (lifeCommand.getPermission() != null && !sender.hasPermission(lifeCommand.getPermission())) {
            sender.sendMessage(lang.getMessage("system.no-permission"));
            return true;
        }

        if (lifeCommand.isPlayerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("system.player-only"));
            return true;
        }

        CommandContext context = new CommandContext(sender, args, plugin);
        lifeCommand.execute(context);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        CommandContext context = new CommandContext(sender, args, plugin);
        List<String> completions = lifeCommand.onTabComplete(context);
        return completions != null ? completions : java.util.Collections.emptyList();
    }
}
