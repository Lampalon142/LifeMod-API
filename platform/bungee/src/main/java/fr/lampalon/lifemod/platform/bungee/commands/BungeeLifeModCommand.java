package fr.lampalon.lifemod.platform.bungee.commands;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashMap;

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

        plugin.reloadBungeeConfig();
        sender.sendMessage(TextComponent.fromLegacyText(
                ServiceRegistry.get(ILangService.class).getMessage("commands.lifemod.reload")));

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            ph.capture("lifemod_reload", new HashMap<>());
        }
    }
}
