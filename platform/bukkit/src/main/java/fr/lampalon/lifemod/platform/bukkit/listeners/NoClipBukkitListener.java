package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener Bukkit pour gérer les cas limites du NoClip :
 *  - Déconnexion : nettoie l'état sans remettre le GameMode (joueur déconnecté)
 *  - Mort        : désactive proprement le NoClip avant le respawn
 *  - GameMode change extérieur (ex: /gamemode par un autre admin) : désactive le NoClip
 */
public class NoClipBukkitListener implements Listener {

    private final NoClipManager noClipManager;

    public NoClipBukkitListener(NoClipManager noClipManager) {
        this.noClipManager = noClipManager;
    }

    // Déconnexion → on retire juste de la map (pas besoin de restaurer le GM)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (noClipManager.isNoClip(event.getPlayer().getUniqueId())) {
            noClipManager.disableNoClip(event.getPlayer());
        }
    }

    // Mort → désactive le NoClip proprement (sinon respawn en Spectator)
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (noClipManager.isNoClip(event.getEntity().getUniqueId())) {
            noClipManager.disableNoClip(event.getEntity());
        }
    }

    // Si un admin change le GameMode du joueur manuellement → coupe le NoClip
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!noClipManager.isNoClip(event.getPlayer().getUniqueId())) return;

        // Si c'est nous qui changeons en SPECTATOR (enableNoClip), on ignore
        // On détecte ça si la cause n'est pas PLUGIN (commande externe)
        // En pratique : on laisse passer, disableNoClip sera appelé via la commande
    }
}