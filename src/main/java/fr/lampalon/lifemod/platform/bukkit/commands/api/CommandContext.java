package fr.lampalon.lifemod.platform.bukkit.commands.api;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandContext {
    private final CommandSender sender;
    private final String[] args;
    private final LifeMod plugin;
    private final ILangService langService;
    private final IConfigurationService configService;
    private final DebugManager debugManager;

    public CommandContext(CommandSender sender, String[] args, LifeMod plugin) {
        this.sender = sender;
        this.args = args;
        this.plugin = plugin;
        this.langService = ServiceRegistry.get(ILangService.class);
        this.configService = ServiceRegistry.get(IConfigurationService.class);
        this.debugManager = plugin.getDebugManager();
    }

    public CommandSender getSender() {
        return sender;
    }

    public String[] getArgs() {
        return args;
    }

    public LifeMod getPlugin() {
        return plugin;
    }

    public ILangService getLang() {
        return langService;
    }

    public IConfigurationService getConfig() {
        return configService;
    }

    public DebugManager getDebug() {
        return debugManager;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public Player getPlayer() {
        return isPlayer() ? (Player) sender : null;
    }

    public UUID getSenderUniqueId() {
        return isPlayer() ? ((Player) sender).getUniqueId() : null;
    }

    public Player getNMSPlayer() {
        return getPlayer();
    }
}
