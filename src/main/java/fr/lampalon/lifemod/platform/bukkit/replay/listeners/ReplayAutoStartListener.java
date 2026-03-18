package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Automatically starts replay recording for players joining the server.
 */
public class ReplayAutoStartListener implements Listener {

    private final LifeMod plugin;

    public ReplayAutoStartListener(LifeMod plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        // Starts recording automatically with a session name based on player name and timestamp
        plugin.getReplayManager().startRecording(playerName + "_" + System.currentTimeMillis());
    }
}
