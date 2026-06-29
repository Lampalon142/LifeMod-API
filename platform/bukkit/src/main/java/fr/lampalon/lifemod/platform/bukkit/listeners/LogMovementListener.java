package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class LogMovementListener extends LogBaseListener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!enabled("log-teleport")) return;
        Player p = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        logAsync(LogEntry.builder()
            .type(LogType.TELEPORT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"c\":\"" + event.getCause().name() + "\",\"fx\":" + from.getBlockX() + ",\"fy\":" + from.getBlockY() + ",\"fz\":" + from.getBlockZ() + ",\"fw\":\"" + locData(from) + "\"}")
            .world(locData(to)).x(to.getBlockX()).y(to.getBlockY()).z(to.getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!enabled("log-world")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.GAMEMODE_CHANGE).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"f\":\"" + p.getGameMode().name() + "\",\"t\":\"" + event.getNewGameMode().name() + "\"}")
            .serverName(serverName()).now().build());
    }
}
