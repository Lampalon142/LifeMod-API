package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class AntiAltListener implements Listener {

    private final LifeMod plugin;
    private final AntiAltManager antiAltManager;

    public AntiAltListener(LifeMod plugin) {
        this.plugin = plugin;
        this.antiAltManager = plugin.getAntiAltManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // L'analyse est maintenant entièrement asynchrone à l'intérieur du manager
        antiAltManager.handlePlayerJoin(event.getPlayer());
    }
}
