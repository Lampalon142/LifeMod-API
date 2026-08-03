package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Automatically starts replay recording for players joining the server.
 */
public class ReplayAutoStartListener implements Listener {

    private final LifeMod plugin;

    public ReplayAutoStartListener(LifeMod plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        
        // Hide any active replay spectators from the new joiner
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!online.equals(joined) && plugin.getReplayPlayerManager().isInReplay(online)) {
                joined.hidePlayer(plugin, online);
            }
        }
        
        String playerName = joined.getName();
        int entityId = joined.getEntityId();
        
        DebugManager debug = plugin.getDebugManager();
        debug.log("replay", "PlayerJoinEvent: " + playerName + " (UUID: " + joined.getUniqueId() + ", ID: " + entityId + ")");
        
        org.bukkit.Location loc = joined.getLocation();
        plugin.getReplayManager().startRecording(
            joined.getUniqueId(), 
            entityId,
            joined.getName(),
            playerName + "_" + System.currentTimeMillis(),
            loc.getWorld().getName(),
            loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        DebugManager debug = plugin.getDebugManager();
        debug.log("replay", "PlayerQuitEvent: " + event.getPlayer().getName());
        plugin.getReplayManager().stopRecording(event.getPlayer().getUniqueId());
    }
}
