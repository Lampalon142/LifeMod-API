package fr.lampalon.lifemod.platform.bungee.listeners;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class BungeeChatListener implements Listener {

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCommand() || event.isProxyCommand()) return;
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;

        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        ILangService lang = ServiceRegistry.get(ILangService.class);
        ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);

        Sanction activeMute = sanctionService.getActiveSanction(player.getUniqueId(), player.getName(), SanctionType.MUTE).join();

        if (activeMute != null) {
            String message = lang.getMessage("sanctions.mute.chat-blocked",
                    "%reason%", activeMute.getReason(),
                    "%expiration%", activeMute.isPermanent() ? lang.getMessage("sanctions.permanent") : TimeUtil.formatTime(activeMute.getExpirationTime() - System.currentTimeMillis()));

            player.sendMessage(new TextComponent(message));
            event.setCancelled(true);
            return;
        }

        if (!config.getBoolean("modules.staffchat.enabled", true)) return;
        if (!player.hasPermission("lifemod.staffchat")) return;

        BungeeLifeMod bungee = BungeeLifeMod.getInstance();
        UUID uuid = player.getUniqueId();

        if (bungee.getStaffChatToggled().contains(uuid)) {
            event.setCancelled(true);

            String rawMsg = event.getMessage();
            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
            String serverName = player.getServer() != null ? player.getServer().getInfo().getName() : platform.getServerName();
            String staffchatMessage = lang.getMessage("commands.staffchat.message",
                    "%player%", player.getName(),
                    "%message%", rawMsg,
                    "%server%", serverName);

            platform.broadcast(staffchatMessage, "lifemod.staffchat");

            IMessagingService messaging = ServiceRegistry.get(IMessagingService.class);
            if (messaging != null) {
                messaging.publish("lifemod:staff", "CHAT|" + uuid + "|" + player.getName() + "|" + rawMsg + "|" + serverName);
            }
        }
    }
}
