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
 *  - External GameMode change away from SPECTATOR: disables NoClip
 */
public class NoClipBukkitListener implements Listener {

    private final NoClipManager noClipManager;

    public NoClipBukkitListener(NoClipManager noClipManager) {
        this.noClipManager = noClipManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        noClipManager.cleanupQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (noClipManager.isNoClip(event.getEntity().getUniqueId())) {
            noClipManager.disableNoClip(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        // Our own enable/disable manage the state; any change leaving SPECTATOR
        // while in NoClip is treated as an external override and disables NoClip.
        if (noClipManager.isNoClip(event.getPlayer().getUniqueId())
                && event.getNewGameMode() != org.bukkit.GameMode.SPECTATOR) {
            noClipManager.disableNoClip(event.getPlayer());
        }
    }
}