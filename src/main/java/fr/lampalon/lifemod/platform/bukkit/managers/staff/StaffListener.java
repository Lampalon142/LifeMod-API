package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.IStaffAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.StaffActionType;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.model.StaffItem;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffListener implements Listener {

    private final StaffModeManager staffModeManager;
    private final StaffItemManager staffItemManager;
    private final StaffActionManager staffActionManager;
    
    // Utilisation d'un cache d'interaction par tick pour bloquer les doublons (NMS/Bukkit)
    private final Map<UUID, Long> lastActionTick = new HashMap<>();

    public StaffListener(StaffModeManager staffModeManager, StaffItemManager staffItemManager, StaffActionManager staffActionManager) {
        this.staffModeManager = staffModeManager;
        this.staffItemManager = staffItemManager;
        this.staffActionManager = staffActionManager;
    }

    /**
     * Vérifie si une action a déjà été traitée pour ce joueur dans ce tick.
     * Compatible avec toutes les versions via l'API de base tout en étant ultra précis.
     */
    private boolean canInteract(Player player) {
        long currentTick = Bukkit.getServer().getWorlds().get(0).getFullTime(); // Plus stable que getCurrentTick sur certaines versions
        if (lastActionTick.containsKey(player.getUniqueId()) && lastActionTick.get(player.getUniqueId()) == currentTick) {
            return false;
        }
        lastActionTick.put(player.getUniqueId(), currentTick);
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        } catch (NoSuchMethodError ignored) {}

        Player player = event.getPlayer();
        if (!staffModeManager.isMod(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) return;

        event.setCancelled(true);

        if (!canInteract(player)) return;

        String clickType = getClickType(event.getAction());
        if (clickType == null) return;

        handleStaffAction(player, staffItem, clickType, event, null);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        } catch (NoSuchMethodError ignored) {}

        Player player = event.getPlayer();
        if (!staffModeManager.isMod(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) return;

        event.setCancelled(true);

        if (!canInteract(player)) return;

        handleStaffAction(player, staffItem, "RIGHT_CLICK", null, event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        
        if (!staffModeManager.isMod(player)) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) return;

        if (!canInteract(player)) return;

        StaffActionType actionType = staffItem.getAction("LEFT_CLICK");
        if (actionType != null) {
            event.setCancelled(true);
            IStaffAction action = staffActionManager.getAction(actionType);
            if (action != null) {
                action.onAttack(player, event.getEntity());
            }
        }
    }

    private void handleStaffAction(Player player, StaffItem staffItem, String clickType, PlayerInteractEvent interactEvent, PlayerInteractEntityEvent entityEvent) {
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
                if (entityEvent != null) {
                    action.onInteractEntity(player, entityEvent);
                } else if (interactEvent != null) {
                    action.onInteract(player, interactEvent);
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
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null && !clickedInventory.equals(player.getInventory())) {
            if (cursorItem != null && staffItemManager.getStaffItem(cursorItem) != null) {
                event.setCancelled(true);
                player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.restrictions.no-external-put")));
                return;
            }
        }

        if (event.getClick().isShiftClick() && clickedInventory != null && clickedInventory.equals(player.getInventory())) {
            if (clickedItem != null && staffItemManager.getStaffItem(clickedItem) != null) {
                Inventory topInv = event.getView().getTopInventory();
                if (topInv != null && !topInv.getType().name().equals("CRAFTING") && !topInv.equals(player.getInventory())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (staffModeManager.isMod(event.getPlayer())) {
            ItemStack dropped = event.getItemDrop().getItemStack();
            if (staffItemManager.getStaffItem(dropped) != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.restrictions.no-drop")));
            }
        }
    }

    private String getClickType(Action action) {
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) return "RIGHT_CLICK";
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) return "LEFT_CLICK";
        return null;
    }
}
