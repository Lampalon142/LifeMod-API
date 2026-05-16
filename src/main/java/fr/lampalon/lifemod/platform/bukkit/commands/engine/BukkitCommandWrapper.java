package fr.lampalon.lifemod.platform.bukkit.commands.engine;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BukkitCommandWrapper extends Command {
    private final LifeCommand lifeCommand;
    private final LifeMod plugin;

    public BukkitCommandWrapper(LifeCommand lifeCommand, LifeMod plugin) {
        super(lifeCommand.getName());
        this.lifeCommand = lifeCommand;
        this.plugin = plugin;
        setAliases(lifeCommand.getAliases());
        setDescription(lifeCommand.getDescription());
        setUsage(lifeCommand.getUsage());
        if (lifeCommand.getPermission() != null) {
            setPermission(lifeCommand.getPermission());
        }
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        ILangService lang = ServiceRegistry.get(ILangService.class);

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
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        CommandContext context = new CommandContext(sender, args, plugin);
        List<String> completions = lifeCommand.onTabComplete(context);
        return completions != null ? completions : java.util.Collections.emptyList();
    }
}
