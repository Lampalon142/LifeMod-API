package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.StaffActionType;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.model.StaffItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class StaffListener implements Listener {

    private final StaffModeManager staffModeManager;
    private final StaffItemManager staffItemManager;
    private final ScriptExecutor scriptExecutor;
    private final DebugManager debug;

    public StaffListener(StaffModeManager staffModeManager, StaffItemManager staffItemManager, StaffActionManager staffActionManager) {
        this.staffModeManager = staffModeManager;
        this.staffItemManager = staffItemManager;
        this.scriptExecutor = new ScriptExecutor(staffActionManager);
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    public void handleQuit(UUID playerId) {
        // no tick map to clean anymore
    }

    // ─── INTERACT (block/air) ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (isOffHand(event)) return;

        if (!staffModeManager.isMod(player)) return;

        event.setCancelled(true);

        StaffItem staffItem = getStaffItemFromHand(player);
        if (staffItem == null) return;

        String clickType = toClickType(event.getAction());
        if (clickType == null) return;

        List<String> scripts = staffItem.getScripts(clickType);
        if (scripts == null || scripts.isEmpty()) return;

        // For RIGHT_CLICK_AIR, skip if the item's scripts contain a NATIVE action
        // that needs an entity/block target — the PlayerInteractEntityEvent will handle it.
        if (event.getAction() == Action.RIGHT_CLICK_AIR && scriptsRequireTarget(scripts)) return;

        StaffActionContext context = new StaffActionContext(player, staffItem, clickType, event, null, null);
        scriptExecutor.execute(context, scripts);
    }

    // ─── INTERACT ENTITY (right-click on player/mob) ────────────────────────

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (isOffHand(event)) return;

        if (!staffModeManager.isMod(player)) return;

        event.setCancelled(true);

        StaffItem staffItem = getStaffItemFromHand(player);
        if (staffItem == null) return;

        List<String> scripts = staffItem.getScripts("RIGHT_CLICK");
        if (scripts == null || scripts.isEmpty()) return;

        StaffActionContext context = new StaffActionContext(player, staffItem, "RIGHT_CLICK", null, event, event.getRightClicked());
        scriptExecutor.execute(context, scripts);
    }

    // ─── ENTITY DAMAGE (left-click attack) ──────────────────────────────────

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        if (!staffModeManager.isMod(player)) return;

        event.setCancelled(true);

        StaffItem staffItem = getStaffItemFromHand(player);
        if (staffItem == null) return;

        List<String> scripts = staffItem.getScripts("LEFT_CLICK");
        if (scripts == null || scripts.isEmpty()) return;

        StaffActionContext context = new StaffActionContext(player, staffItem, "LEFT_CLICK", null, null, event.getEntity());
        scriptExecutor.execute(context, scripts);
    }

    // ─── INVENTORY CLICK ────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!staffModeManager.isMod(player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        Inventory clickedInv = event.getClickedInventory();

        // Block putting staff items into external inventories
        if (clickedInv != null && !clickedInv.equals(player.getInventory())) {
            if (cursorItem != null && staffItemManager.getStaffItem(cursorItem) != null) {
                event.setCancelled(true);
                ILangService lang = ServiceRegistry.get(ILangService.class);
                player.sendMessage(lang.getMessage("mod.items.restrictions.no-external-put"));
                return;
            }
        }

        // Block shift-clicking staff items out of player inventory
        if (event.getClick().isShiftClick() && clickedInv != null && clickedInv.equals(player.getInventory())) {
            if (clickedItem != null && staffItemManager.getStaffItem(clickedItem) != null) {
                Inventory topInv = event.getView().getTopInventory();
                if (topInv != null && !topInv.getType().name().equals("CRAFTING") && !topInv.equals(player.getInventory())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ─── DROP ───────────────────────────────────────────────────────────────

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!staffModeManager.isMod(event.getPlayer())) return;

        ItemStack dropped = event.getItemDrop().getItemStack();
        if (staffItemManager.getStaffItem(dropped) != null) {
            event.setCancelled(true);
            ILangService lang = ServiceRegistry.get(ILangService.class);
            event.getPlayer().sendMessage(lang.getMessage("mod.items.restrictions.no-drop"));
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────

    private boolean isOffHand(PlayerInteractEvent event) {
        try {
            return event.getHand() == EquipmentSlot.OFF_HAND;
        } catch (NoSuchMethodError e) {
            return false;
        }
    }

    private boolean isOffHand(PlayerInteractEntityEvent event) {
        try {
            return event.getHand() == EquipmentSlot.OFF_HAND;
        } catch (NoSuchMethodError e) {
            return false;
        }
    }

    private StaffItem getStaffItemFromHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return null;
        return staffItemManager.getStaffItem(item);
    }

    private String toClickType(Action action) {
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) return "RIGHT_CLICK";
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) return "LEFT_CLICK";
        return null;
    }

    private boolean scriptsRequireTarget(List<String> scripts) {
        for (String script : scripts) {
            String upper = script.toUpperCase().trim();
            String actionPart;
            if (upper.startsWith("[NATIVE]")) {
                actionPart = script.substring(8).trim();
            } else {
                actionPart = script.trim();
            }
            StaffActionType type = StaffActionType.fromString(actionPart);
            if (type != null && type.requiresTarget()) return true;
        }
        return false;
    }
}
