package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.logging.Logger;

/**
 * Automatically starts replay recording for players joining the server.
 */
public class ReplayAutoStartListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger("ReplayAutoStartListener");
    private final LifeMod plugin;

    public ReplayAutoStartListener(LifeMod plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        int entityId = event.getPlayer().getEntityId();
        
        LOGGER.info("[DEBUG] PlayerJoinEvent: " + playerName + " (UUID: " + event.getPlayer().getUniqueId() + ", ID: " + entityId + ")");
        
        plugin.getReplayManager().startRecording(
            event.getPlayer().getUniqueId(), 
            entityId,
            playerName + "_" + System.currentTimeMillis()
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        LOGGER.info("[DEBUG] PlayerQuitEvent: " + event.getPlayer().getName());
        plugin.getReplayManager().stopRecording(event.getPlayer().getUniqueId());
    }
}
