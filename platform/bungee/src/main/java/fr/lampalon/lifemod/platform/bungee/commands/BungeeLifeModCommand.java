package fr.lampalon.lifemod.platform.bungee.commands;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class BungeeLifeModCommand extends Command {

    private final BungeeLifeMod plugin;

    public BungeeLifeModCommand(BungeeLifeMod plugin) {
        super("lifemod", "lifemod.admin");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(TextComponent.fromLegacyText("§cUsage: /lifemod reload"));
            return;
        }

        if (!sender.hasPermission("lifemod.admin")) {
            sender.sendMessage(TextComponent.fromLegacyText(
                    ServiceRegistry.get(ILangService.class).getMessage("system.no-permission")));
            return;
        }

        plugin.fullReload();
        sender.sendMessage(TextComponent.fromLegacyText(
                ServiceRegistry.get(ILangService.class).getMessage("commands.lifemod.reload")));

        IMessagingService messaging = ServiceRegistry.get(IMessagingService.class);
        if (messaging != null) {
            messaging.publish("lifemod:reload", "RELOAD");
            plugin.getLogger().info("Published cross-server reload signal to all Bukkit servers.");
        }
    }
}
