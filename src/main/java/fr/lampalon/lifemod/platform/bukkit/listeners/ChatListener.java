package fr.lampalon.lifemod.platform.bukkit.listeners;

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
                    plugin.getLangConfig().getString("togglechat.blocked")
            ));
            event.setCancelled(true);
        }
    }
}


