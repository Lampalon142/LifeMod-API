package fr.lampalon.lifemod.platform.bungee.commands;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BungeeStaffchatCommand extends Command {

    private final BungeeLifeMod plugin;

    public BungeeStaffchatCommand(BungeeLifeMod plugin) {
        super("staffchat", "lifemod.staffchat");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        if (!config.getBoolean("modules.staffchat.enabled", true)) {
            sender.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                    ServiceRegistry.get(ILangService.class).getMessage("commands.staffchat.usage")));
            return;
        }

        if (!(sender instanceof ProxiedPlayer player)) {
            sender.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                    ServiceRegistry.get(ILangService.class).getMessage("system.player-only")));
            return;
        }

        ILangService lang = ServiceRegistry.get(ILangService.class);
        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);

        if (args.length == 0) {
            Set<UUID> toggled = plugin.getStaffChatToggled();
            UUID uuid = player.getUniqueId();
            if (toggled.contains(uuid)) {
                toggled.remove(uuid);
                player.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        lang.getMessage("commands.staffchat.toggled_off")));
            } else {
                toggled.add(uuid);
                player.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        lang.getMessage("commands.staffchat.toggled_on")));
            }
            return;
        }

        String message = String.join(" ", args);
        String serverName = player.getServer() != null ? player.getServer().getInfo().getName() : platform.getServerName();
        String staffchatMessage = lang.getMessage("commands.staffchat.message",
                "%player%", player.getName(),
                "%message%", message,
                "%server%", serverName);

        platform.broadcast(staffchatMessage, "lifemod.staffchat");

        IMessagingService messaging = ServiceRegistry.get(IMessagingService.class);
        if (messaging != null) {
            messaging.publish("lifemod:staff", "CHAT|" + player.getUniqueId() + "|" + player.getName() + "|" + message + "|" + serverName);
        }

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("word_count", message.split("\\s+").length);
            props.put("platform", "bungee");
            ph.capture("lifemod_staff_chat", props);
        }
    }
}
