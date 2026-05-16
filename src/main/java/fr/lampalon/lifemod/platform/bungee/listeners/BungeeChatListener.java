package fr.lampalon.lifemod.platform.bungee.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeChatListener implements Listener {

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCommand() || event.isProxyCommand()) return;
        if (!(event.getSender() instanceof ProxiedPlayer)) return;

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
        ILangService lang = ServiceRegistry.get(ILangService.class);

        Sanction activeMute = sanctionService.getActiveSanction(player.getUniqueId(), player.getName(), SanctionType.MUTE).join();

        if (activeMute != null) {
            String message = lang.getMessage("sanctions.mute.chat-blocked",
                    "%reason%", activeMute.getReason(),
                    "%expiration%", activeMute.isPermanent() ? "Jamais" : TimeUtil.formatTime(activeMute.getExpirationTime() - System.currentTimeMillis()));
            
            player.sendMessage(new TextComponent(message));
            event.setCancelled(true);
        }
    }
}
