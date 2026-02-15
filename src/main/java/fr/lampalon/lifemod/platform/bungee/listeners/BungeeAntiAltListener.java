package fr.lampalon.lifemod.platform.bungee.listeners;

import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import fr.lampalon.lifemod.platform.bungee.managers.antialt.BungeeAntiAltManager;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeAntiAltListener implements Listener {

    private final BungeeLifeMod plugin;
    private final BungeeAntiAltManager antiAltManager;

    public BungeeAntiAltListener(BungeeLifeMod plugin) {
        this.plugin = plugin;
        this.antiAltManager = plugin.getAntiAltManager();
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        // The event is already async, so we can directly call our manager
        antiAltManager.handleConnection(event.getConnection());
    }
}
