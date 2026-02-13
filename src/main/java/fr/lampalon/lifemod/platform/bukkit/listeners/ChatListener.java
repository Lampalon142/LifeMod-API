package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final LifeMod plugin = LifeMod.getInstance();

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.isChatEnabled() && !event.getPlayer().hasPermission("lifemod.togglechat.bypass")) {
            event.getPlayer().sendMessage(MessageUtil.formatMessage(
                    plugin.getLangConfig().getString("commands.chat.blocked")
            ));
            event.setCancelled(true);
            return;
        }

        ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
        Sanction activeMute = sanctionService.getActiveSanction(event.getPlayer().getUniqueId(), event.getPlayer().getName(), SanctionType.MUTE).join();

        if (activeMute != null) {
            fr.lampalon.lifemod.common.service.ILangService langService = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            String reason = activeMute.getReason();
            String time = activeMute.isPermanent() ? "Permanent" : TimeUtil.formatTime(activeMute.getExpirationTime() - System.currentTimeMillis());

            String message = langService.getMessage("mute.blocked-message",
                    "%reason%", reason,
                    "%time%", time
            );

            event.getPlayer().sendMessage(message);
            event.setCancelled(true);
        }
    }
}


