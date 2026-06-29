package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.managers.staff.IVanishService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.Iterator;

public class ServerListPingListener implements Listener {

    private final IVanishService vanishService;

    public ServerListPingListener(IVanishService vanishService) {
        this.vanishService = vanishService;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        Iterator<Player> iterator = event.iterator();

        while (iterator.hasNext()) {
            Player player = iterator.next();

            if (vanishService.isVanished(player.getUniqueId())) {
                iterator.remove();
            }
        }
    }
}