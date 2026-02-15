package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
        Player player = event.getPlayer();
        
        // Don't block the main thread for analysis
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            antiAltManager.analyze(player);
        });
    }
}
