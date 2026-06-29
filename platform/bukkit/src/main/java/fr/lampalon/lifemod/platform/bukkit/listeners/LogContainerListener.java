package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LogContainerListener extends LogBaseListener {

    private final Map<UUID, Long> containerOpenSince = new HashMap<>();
    private final Map<UUID, Inventory> openContainers = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerOpen(PlayerInteractEvent event) {
        if (!enabled("log-container")) return;
        if (event.getClickedBlock() == null) return;
        BlockState state = event.getClickedBlock().getState();
        if (state instanceof Container) {
            Player p = event.getPlayer();
            Location loc = event.getClickedBlock().getLocation();
            logAsync(LogEntry.builder()
                .type(LogType.CONTAINER_OPEN).playerUuid(p.getUniqueId()).playerName(p.getName())
                .actionData("{\"c\":\"" + state.getType().name() + "\"}")
                .world(locData(loc)).x(loc.getBlockX()).y(loc.getBlockY()).z(loc.getBlockZ())
                .serverName(serverName()).now().build());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled("log-container")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        Inventory top = event.getView().getTopInventory();
        if (top == null || top.getType() == InventoryType.PLAYER || top.getType() == InventoryType.CRAFTING) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (current == null && cursor == null) return;

        LogType logType = null;
        String itemName = null;
        int amount = 0;

        if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
            if (event.getRawSlot() < top.getSize()) {
                if (current != null && current.getType() != Material.AIR) {
                    logType = LogType.CONTAINER_TAKE;
                    itemName = current.getType().name();
                    amount = current.getAmount();
                }
            } else {
                if (cursor != null && cursor.getType() != Material.AIR) {
                    logType = LogType.CONTAINER_PUT;
                    itemName = cursor.getType().name();
                    amount = cursor.getAmount();
                } else if (current != null && current.getType() != Material.AIR) {
                    logType = LogType.CONTAINER_TAKE;
                    itemName = current.getType().name();
                    amount = current.getAmount();
                }
            }
        }

        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            if (current != null && current.getType() != Material.AIR) {
                if (event.getRawSlot() < top.getSize()) {
                    logType = LogType.CONTAINER_TAKE;
                } else {
                    logType = LogType.CONTAINER_PUT;
                }
                itemName = current.getType().name();
                amount = current.getAmount();
            }
        }

        if (logType != null) {
            logAsync(LogEntry.builder()
                .type(logType).playerUuid(p.getUniqueId()).playerName(p.getName())
                .actionData("{\"i\":\"" + itemName + "\",\"a\":" + amount + ",\"c\":\"" + top.getType().name() + "\"}")
                .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
                .serverName(serverName()).now().build());
        }
    }
}
