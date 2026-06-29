package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class LogEntityListener extends LogBaseListener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!enabled("log-entity")) return;
        if (!(event.getOwner() instanceof Player)) return;
        Player p = (Player) event.getOwner();
        logAsync(LogEntry.builder()
            .type(LogType.ENTITY_TAME).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"e\":\"" + event.getEntity().getType().name() + "\"}")
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (!enabled("log-entity")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.SHEEP_SHEAR).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"e\":\"" + event.getEntity().getType().name() + "\"}")
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!enabled("log-entity")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.ENTITY_INTERACT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .targetName(event.getRightClicked().getType().name())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!enabled("log-entity")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.FISHING).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"s\":\"" + event.getState().name() + "\"}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!enabled("log-entity")) return;
        if (!(event.getEntered() instanceof Player)) return;
        Player p = (Player) event.getEntered();
        logAsync(LogEntry.builder()
            .type(LogType.VEHICLE_ENTER).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"v\":\"" + event.getVehicle().getType().name() + "\"}")
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!enabled("log-entity")) return;
        if (!(event.getExited() instanceof Player)) return;
        Player p = (Player) event.getExited();
        logAsync(LogEntry.builder()
            .type(LogType.VEHICLE_EXIT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"v\":\"" + event.getVehicle().getType().name() + "\"}")
            .serverName(serverName()).now().build());
    }
}
