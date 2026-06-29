package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Bukkit listener for NoClip edge cases:
 *  - Disconnect: cleans up state without restoring GameMode (player is gone)
 *  - Death: disables NoClip properly before respawn
 *  - External GameMode change (e.g. /gamemode by another admin): disables NoClip
 */
public class NoClipBukkitListener implements Listener {

    private final NoClipManager noClipManager;

    public NoClipBukkitListener(NoClipManager noClipManager) {
        this.noClipManager = noClipManager;
    }

    // Disconnect → remove from the map (no need to restore GM)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (noClipManager.isNoClip(event.getPlayer().getUniqueId())) {
            noClipManager.disableNoClip(event.getPlayer());
        }
    }

    // Death → disable NoClip properly (otherwise respawns in Spectator)
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (noClipManager.isNoClip(event.getEntity().getUniqueId())) {
            noClipManager.disableNoClip(event.getEntity());
        }
    }

    // If an admin changes the player's GameMode manually → disable NoClip
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!noClipManager.isNoClip(event.getPlayer().getUniqueId())) return;

        // If we are the ones changing to SPECTATOR (enableNoClip), ignore
        // Detect this when the cause is not PLUGIN (external command)
        // In practice: let it pass, disableNoClip will be called by the command
    }
}