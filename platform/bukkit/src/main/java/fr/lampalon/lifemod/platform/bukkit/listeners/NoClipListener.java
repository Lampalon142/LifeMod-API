package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class NoClipListener implements Listener {

    private final NoClipManager noClipManager;

    public NoClipListener(NoClipManager noClipManager) {
        this.noClipManager = noClipManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        noClipManager.handleQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        noClipManager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!noClipManager.isNoClip(event.getPlayer().getUniqueId())) return;
        GameMode target = event.getNewGameMode();
        if (target != GameMode.SPECTATOR) {
            noClipManager.disableNoClip(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (noClipManager.isNoClip(event.getPlayer().getUniqueId())) {
            noClipManager.disableNoClip(event.getPlayer());
        }
    }
}