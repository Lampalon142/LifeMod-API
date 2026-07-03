package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class LogDeathListener extends LogBaseListener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!enabled("log-death")) return;
        Player p = event.getEntity();
        LogType type;
        String data;
        if (p.getKiller() != null) {
            type = LogType.DEATH_PLAYER;
            data = "{\"k\":\"" + p.getKiller().getName() + "\",\"w\":\"" + p.getKiller().getInventory().getItemInMainHand().getType().name() + "\"}";
            Location loc = p.getLocation();
            logAsync(LogEntry.builder()
                .type(LogType.KILL_PLAYER).playerUuid(p.getKiller().getUniqueId()).playerName(p.getKiller().getName())
                .targetName(p.getName())
                .actionData("{\"w\":\"" + p.getKiller().getInventory().getItemInMainHand().getType().name() + "\"}")
                .world(locData(loc)).x(loc.getBlockX()).y(loc.getBlockY()).z(loc.getBlockZ())
                .serverName(serverName()).now().build());
        } else if (p.getLastDamageCause() != null) {
            switch (p.getLastDamageCause().getCause()) {
                case FALL: case FLY_INTO_WALL: type = LogType.DEATH_ENVIRONMENT; data = "{\"c\":\"fall\"}"; break;
                case DROWNING: type = LogType.DEATH_ENVIRONMENT; data = "{\"c\":\"drown\"}"; break;
                case LAVA: case FIRE: case FIRE_TICK: type = LogType.DEATH_ENVIRONMENT; data = "{\"c\":\"fire\"}"; break;
                case SUFFOCATION: type = LogType.DEATH_ENVIRONMENT; data = "{\"c\":\"suffocate\"}"; break;
                case ENTITY_ATTACK: case ENTITY_SWEEP_ATTACK:
                    type = LogType.DEATH_MOB;
                    data = p.getLastDamageCause().getEntity() != null
                        ? "{\"m\":\"" + p.getLastDamageCause().getEntity().getType().name() + "\"}"
                        : "{}";
                    break;
                default: type = LogType.DEATH_ENVIRONMENT;
                    data = "{\"c\":\"" + p.getLastDamageCause().getCause().name() + "\"}";
            }
        } else {
            type = LogType.DEATH_OTHER;
            data = "{}";
        }
        Location loc = p.getLocation();
        logAsync(LogEntry.builder()
            .type(type).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData(data).world(locData(loc)).x(loc.getBlockX()).y(loc.getBlockY()).z(loc.getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKillMob(EntityDeathEvent event) {
        if (!enabled("log-death")) return;
        if (event.getEntity() instanceof Player) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        Location loc = event.getEntity().getLocation();
        logAsync(LogEntry.builder()
            .type(LogType.KILL_MOB).playerUuid(killer.getUniqueId()).playerName(killer.getName())
            .targetName(event.getEntity().getType().name())
            .actionData("{\"m\":\"" + event.getEntity().getType().name() + "\"}")
            .world(locData(loc)).x(loc.getBlockX()).y(loc.getBlockY()).z(loc.getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!enabled("log-death")) return;
        Player p = event.getPlayer();
        Location loc = event.getRespawnLocation();
        logAsync(LogEntry.builder()
            .type(LogType.RESPAWN).playerUuid(p.getUniqueId()).playerName(p.getName())
            .world(locData(loc)).x(loc.getBlockX()).y(loc.getBlockY()).z(loc.getBlockZ())
            .serverName(serverName()).now().build());
    }
}
