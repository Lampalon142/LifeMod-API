package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.InvseeManager;
import fr.lampalon.lifemod.platform.bukkit.utils.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
        if (!(event.getPlayer() instanceof Player viewer)) return;

        if (invseeManager.isOfflineViewing(viewer)) {
            saveOfflineInventory(viewer, event.getInventory());
            return;
        }

        invseeManager.stopViewing(viewer);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        if (invseeManager.isOfflineViewing(viewer)) {
            if (!viewer.hasPermission("lifemod.invsee.interact")) {
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                    event.setCancelled(true);
                }
            }
            return;
        }

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
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            }
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> syncInventory(event.getView().getTopInventory(), target));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        if (invseeManager.isOfflineViewing(viewer)) {
            if (!viewer.hasPermission("lifemod.invsee.interact")) {
                event.setCancelled(true);
            }
            return;
        }

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
        updateViewers((Player) event.getWhoClicked());
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (invseeManager.isOfflineInvLocked(uuid)) {
            String targetName = event.getPlayer().getName();
            for (Player online : Bukkit.getOnlinePlayers()) {
                UUID viewed = invseeManager.getOfflineTargetUUID(online);
                if (uuid.equals(viewed)) {
                    online.sendMessage("§c" + targetName + " logged in while you were editing their offline inventory. Changes discarded.");
                }
            }
            invseeManager.releaseForTarget(uuid);
        }
    }

    private void saveOfflineInventory(Player viewer, Inventory inv) {
        UUID targetUuid = invseeManager.stopOfflineViewing(viewer);
        if (targetUuid == null) return;

        Player onlineTarget = Bukkit.getPlayer(targetUuid);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            viewer.sendMessage("§cChanges not saved: " + onlineTarget.getName() + " is now online.");
            return;
        }

        if (inv.getSize() != 45) return;

        try {
            ItemStack[] contents = new ItemStack[36];
            for (int i = 0; i < 36; i++) contents[i] = inv.getItem(i);

            ItemStack[] armor = new ItemStack[4];
            armor[3] = inv.getItem(36);
            armor[2] = inv.getItem(37);
            armor[1] = inv.getItem(38);
            armor[0] = inv.getItem(39);

            byte[] data = InventoryUtil.serializeInventory(contents, armor);
            String serverName = plugin.getServerName();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getDatabaseManager().getDatabaseProvider().saveRawInventory(targetUuid, serverName, data));

            String name = Bukkit.getOfflinePlayer(targetUuid).getName();
            viewer.sendMessage("§aOffline inventory saved for " + (name != null ? name : targetUuid.toString().substring(0, 8)));
        } catch (Exception e) {
            viewer.sendMessage("§cFailed to save offline inventory: " + e.getMessage());
            plugin.getDebugManager().log("invsee", "Save error for " + targetUuid + ": " + e.getMessage());
        }
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
