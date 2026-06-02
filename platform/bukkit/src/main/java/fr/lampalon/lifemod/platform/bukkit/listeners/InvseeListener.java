package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.InvseeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public class InvseeListener implements Listener {

    private final LifeMod plugin;
    private final InvseeManager invseeManager;

    public InvseeListener(LifeMod plugin) {
        this.plugin = plugin;
        this.invseeManager = plugin.getInvseeManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            invseeManager.stopViewing((Player) event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player viewer = (Player) event.getWhoClicked();

        if (!invseeManager.isViewing(viewer)) return;

        UUID targetUUID = invseeManager.getTargetUUID(viewer);
        if (targetUUID == null) return;

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            invseeManager.stopViewing(viewer);
            viewer.closeInventory();
            return;
        }

        // Check permission if needed
        if (!viewer.hasPermission("lifemod.invsee.interact")) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            }
            return;
        }

        // Sync logic
        Bukkit.getScheduler().runTask(plugin, () -> syncInventory(event.getView().getTopInventory(), target));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player viewer = (Player) event.getWhoClicked();

        if (!invseeManager.isViewing(viewer)) return;

        UUID targetUUID = invseeManager.getTargetUUID(viewer);
        if (targetUUID == null) return;

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            invseeManager.stopViewing(viewer);
            viewer.closeInventory();
            return;
        }

        if (!viewer.hasPermission("lifemod.invsee.interact")) {
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncInventory(event.getView().getTopInventory(), target));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player target = (Player) event.getWhoClicked();

        updateViewers(target);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        updateViewers((Player) event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetDrop(PlayerDropItemEvent event) {
        updateViewers(event.getPlayer());
    }

    private void updateViewers(Player target) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (invseeManager.isViewing(viewer)) {
                    UUID targetUUID = invseeManager.getTargetUUID(viewer);
                    if (target.getUniqueId().equals(targetUUID)) {
                        updateCustomInventory(viewer.getOpenInventory().getTopInventory(), target);
                    }
                }
            }
        });
    }

    private void updateCustomInventory(Inventory customInv, Player target) {
        PlayerInventory targetInv = target.getInventory();
        for (int i = 0; i < 36; i++) {
            customInv.setItem(i, targetInv.getItem(i));
        }
        customInv.setItem(36, targetInv.getHelmet());
        customInv.setItem(37, targetInv.getChestplate());
        customInv.setItem(38, targetInv.getLeggings());
        customInv.setItem(39, targetInv.getBoots());
    }

    private void syncInventory(Inventory customInv, Player target) {
        PlayerInventory targetInv = target.getInventory();

        for (int i = 0; i < 36; i++) {
            targetInv.setItem(i, customInv.getItem(i));
        }
        targetInv.setHelmet(customInv.getItem(36));
        targetInv.setChestplate(customInv.getItem(37));
        targetInv.setLeggings(customInv.getItem(38));
        targetInv.setBoots(customInv.getItem(39));
        
        target.updateInventory();
    }
}
