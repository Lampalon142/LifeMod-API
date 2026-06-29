package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class LogBlockListener extends LogBaseListener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled("log-block")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.BLOCK_BREAK).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"b\":\"" + event.getBlock().getType().name() + "\"}")
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!enabled("log-block")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.BLOCK_PLACE).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"b\":\"" + event.getBlock().getType().name() + "\"}")
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!enabled("log-block")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.BUCKET_FILL).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"b\":\"" + event.getBucket().name() + "\"}")
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!enabled("log-block")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.BUCKET_EMPTY).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"b\":\"" + event.getBucket().name() + "\"}")
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!enabled("log-block")) return;
        Player p = event.getPlayer();
        String[] lines = event.getLines();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 4); i++) {
            if (lines[i] != null && !lines[i].isEmpty()) {
                if (text.length() > 0) text.append("\\n");
                text.append(jsonEscape(lines[i]));
            }
        }
        logAsync(LogEntry.builder()
            .type(LogType.SIGN_CHANGE).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"t\":\"" + text + "\"}")
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (!enabled("log-block")) return;
        if (event.getPlayer() == null) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.BLOCK_IGNITE).playerUuid(p.getUniqueId()).playerName(p.getName())
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!enabled("log-block")) return;
        logAsync(LogEntry.builder()
            .type(LogType.BLOCK_BURN)
            .actionData("{\"b\":\"" + event.getBlock().getType().name() + "\"}")
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (!enabled("log-block")) return;
        logAsync(LogEntry.builder()
            .type(LogType.BLOCK_FADE)
            .actionData("{\"b\":\"" + event.getBlock().getType().name() + "\"}")
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        if (!enabled("log-block")) return;
        logAsync(LogEntry.builder()
            .type(LogType.BLOCK_FORM)
            .actionData("{\"f\":\"" + event.getNewState().getType().name() + "\",\"s\":\"" + event.getBlock().getType().name() + "\"}")
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!enabled("log-block")) return;
        logAsync(LogEntry.builder()
            .type(LogType.LEAVES_DECAY)
            .world(locData(event.getBlock().getLocation()))
            .x(event.getBlock().getX()).y(event.getBlock().getY()).z(event.getBlock().getZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!enabled("log-block")) return;
        Location loc = event.getLocation();
        logAsync(LogEntry.builder()
            .type(LogType.EXPLOSION)
            .actionData("{\"e\":\"" + event.getEntityType().name() + "\",\"s\":" + event.blockList().size() + "}")
            .world(locData(loc)).x(loc.getBlockX()).y(loc.getBlockY()).z(loc.getBlockZ())
            .serverName(serverName()).now().build());
    }
}
