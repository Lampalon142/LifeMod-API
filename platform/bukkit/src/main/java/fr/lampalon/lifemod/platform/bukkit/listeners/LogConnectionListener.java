package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LogConnectionListener extends LogBaseListener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled("log-connection")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.LOGIN_SUCCESS).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"ip\":\"" + p.getAddress() + "\"}").serverName(serverName())
            .now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled("log-connection")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.QUIT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .serverName(serverName()).now().build());
    }
}
