package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.IStaffAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.StaffActionType;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.model.StaffItem;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class StaffListener implements Listener {

    private final StaffModeManager staffModeManager;
    private final StaffItemManager staffItemManager;
    private final StaffActionManager staffActionManager;

    public StaffListener(StaffModeManager staffModeManager, StaffItemManager staffItemManager, StaffActionManager staffActionManager) {
        this.staffModeManager = staffModeManager;
        this.staffItemManager = staffItemManager;
        this.staffActionManager = staffActionManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!staffModeManager.isMod(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) return;

        event.setCancelled(true);

        String clickType = getClickType(event.getAction());
        if (clickType == null) return;

        // Execute Command
        String command = staffItem.getCommand(clickType);
        if (command != null) {
            player.performCommand(command.replace("/", "").replace("%player%", player.getName()));
        }

        // Execute Action
        StaffActionType actionType = staffItem.getAction(clickType);
        if (actionType != null) {
            IStaffAction action = staffActionManager.getAction(actionType);
            if (action != null) {
                action.onInteract(player, event);
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        handleEntityInteract(player, event.getRightClicked(), "RIGHT_CLICK", event);
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        
        // Check if player is mod
        if (!staffModeManager.isMod(player)) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) return;
        
        // If it's a staff item, likely we want to cancel real damage?
        // Depends on action. But usually yes.
        // Let's cancel by default if an action is found.
        
        String clickType = "LEFT_CLICK";
        StaffActionType actionType = staffItem.getAction(clickType);
        
        if (actionType != null) {
            event.setCancelled(true); // Prevent damage
            IStaffAction action = staffActionManager.getAction(actionType);
            if (action != null) {
                action.onAttack(player, event.getEntity());
            }
        }
    }

    private void handleEntityInteract(Player player, org.bukkit.entity.Entity target, String clickType, org.bukkit.event.Cancellable event) {
        if (!staffModeManager.isMod(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) return;

        event.setCancelled(true);

        StaffActionType actionType = staffItem.getAction(clickType);
        if (actionType != null) {
            IStaffAction action = staffActionManager.getAction(actionType);
            if (action != null) {
                // Safe cast or change signature
                if (event instanceof PlayerInteractEntityEvent) {
                    action.onInteractEntity(player, (PlayerInteractEntityEvent) event);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (!staffModeManager.isMod(player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();

        // 1. Prevent putting staff items into external inventories (Chests, Players, etc.)
        if (clickedInventory != null && !clickedInventory.equals(player.getInventory())) {
            if (cursorItem != null && staffItemManager.getStaffItem(cursorItem) != null) {
                event.setCancelled(true);
                player.sendMessage(MessageUtil.formatMessage("&cYou cannot put staff tools in other inventories."));
                return;
            }
        }

        // 2. Prevent Shift-Clicking staff items into external inventories
        if (event.getClick().isShiftClick() && clickedInventory != null && clickedInventory.equals(player.getInventory())) {
            if (clickedItem != null && staffItemManager.getStaffItem(clickedItem) != null) {
                Inventory topInv = event.getView().getTopInventory();
                if (topInv != null && !topInv.getType().name().equals("CRAFTING") && !topInv.equals(player.getInventory())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // 3. Allow staff to take items FROM other inventories (like InvSee)
        // This is allowed by default if we don't cancel it here.
        // We only restrict staff items.
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (staffModeManager.isMod(event.getPlayer())) {
            ItemStack dropped = event.getItemDrop().getItemStack();
            if (staffItemManager.getStaffItem(dropped) != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(MessageUtil.formatMessage("&cYou cannot drop staff tools."));
            }
        }
    }

    private String getClickType(Action action) {
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) return "RIGHT_CLICK";
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) return "LEFT_CLICK";
        return null;
    }
}
