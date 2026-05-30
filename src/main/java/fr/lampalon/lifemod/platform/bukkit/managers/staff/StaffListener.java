package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.BukkitPlatform;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.IStaffAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.StaffActionType;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.model.StaffItem;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StaffListener implements Listener {

    private final StaffModeManager staffModeManager;
    private final StaffItemManager staffItemManager;
    private final StaffActionManager staffActionManager;
    
    private final Map<UUID, Long> lastEntityInteractTick = new HashMap<>();

    public void handleQuit(UUID playerId) {
        lastEntityInteractTick.remove(playerId);
    }

    public StaffListener(StaffModeManager staffModeManager, StaffItemManager staffItemManager, StaffActionManager staffActionManager) {
        this.staffModeManager = staffModeManager;
        this.staffItemManager = staffItemManager;
        this.staffActionManager = staffActionManager;
    }

    private boolean canEntityInteract(Player player) {
        long currentTick = Bukkit.getServer().getWorlds().get(0).getFullTime();
        if (lastEntityInteractTick.containsKey(player.getUniqueId()) && lastEntityInteractTick.get(player.getUniqueId()) == currentTick) {
            return false;
        }
        lastEntityInteractTick.put(player.getUniqueId(), currentTick);
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        } catch (NoSuchMethodError ignored) {}

        Player player = event.getPlayer();
        if (!staffModeManager.isMod(player)) return;

        // When right-clicking air while targeting a player, PlayerInteractEntityEvent
        // handles the actual entity action. Skipping RIGHT_CLICK_AIR prevents a
        // spurious onInteract() call (e.g. "no target") before the entity handler fires.
        if (event.getAction() == Action.RIGHT_CLICK_AIR) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) return;

        event.setCancelled(true);

        String clickType = getClickType(event.getAction());
        if (clickType == null) return;

        handleStaffAction(player, staffItem, clickType, event, null, null);
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

        if (!canEntityInteract(player)) return;

        handleStaffAction(player, staffItem, "RIGHT_CLICK", null, event, event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        
        if (!staffModeManager.isMod(player)) return;
        
        ItemStack item = player.getInventory().getItemInMainHand();
        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) return;

        if (!canEntityInteract(player)) return;

        // On annule les dégâts si une action est prévue
        List<String> scripts = staffItem.getScripts("LEFT_CLICK");
        if (scripts != null && !scripts.isEmpty()) {
            event.setCancelled(true);
            handleStaffAction(player, staffItem, "LEFT_CLICK", null, null, event.getEntity());
        }
    }

    private void handleStaffAction(Player player, StaffItem staffItem, String clickType, PlayerInteractEvent interactEvent, PlayerInteractEntityEvent entityInteractEvent, Entity target) {
        List<String> scripts = staffItem.getScripts(clickType);
        if (scripts == null || scripts.isEmpty()) return;

        for (String script : scripts) {
            executeScript(player, script, interactEvent, entityInteractEvent, target);
        }
    }

    private void executeScript(Player player, String script, PlayerInteractEvent interactEvent, PlayerInteractEntityEvent entityInteractEvent, Entity target) {
        String upper = script.toUpperCase();
        
        if (upper.startsWith("[PLAYER]")) {
            String cmd = script.substring(8).trim().replace("%player%", player.getName()).replace("%player_name%", player.getName());
            player.performCommand(cmd);
        } 
        else if (upper.startsWith("[CONSOLE]")) {
            String cmd = script.substring(9).trim().replace("%player%", player.getName()).replace("%player_name%", player.getName());
            if (target != null) {
                cmd = cmd.replace("%target%", target.getName());
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        else if (upper.startsWith("[MESSAGE]")) {
            fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            player.sendMessage(lang.getMessage(script.substring(9).trim()));
        }
        else if (upper.startsWith("[ACTIONBAR]")) {
            fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
            if (platform instanceof BukkitPlatform) {
                NMSProvider nms = ((BukkitPlatform) platform).getNmsProvider();
                if (nms != null) {
                    nms.sendActionBar(player, lang.getMessage(script.substring(11).trim()));
                }
            }
        }
        else if (upper.startsWith("[SOUND]")) {
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(script.substring(7).trim().toUpperCase());
                player.playSound(player.getLocation(), sound, 1f, 1f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (upper.startsWith("[NATIVE]")) {
            String actionName = script.substring(8).trim().toUpperCase();
            StaffActionType actionType = StaffActionType.fromString(actionName);
            if (actionType != null) {
                executeNativeAction(player, actionType, interactEvent, entityInteractEvent, target);
            }
        }
        else {
            StaffActionType actionType = StaffActionType.fromString(script.trim());
            if (actionType != null) {
                executeNativeAction(player, actionType, interactEvent, entityInteractEvent, target);
            }
        }
    }

    private void executeNativeAction(Player player, StaffActionType actionType, PlayerInteractEvent interactEvent, PlayerInteractEntityEvent entityInteractEvent, Entity target) {
        IStaffAction action = staffActionManager.getAction(actionType);
        if (action == null) return;

        if (entityInteractEvent != null) {
            action.onInteractEntity(player, entityInteractEvent);
        } else if (interactEvent != null) {
            action.onInteract(player, interactEvent);
        } else if (target != null) {
            action.onAttack(player, target);
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
                fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                player.sendMessage(lang.getMessage("mod.items.restrictions.no-external-put"));
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
                fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                event.getPlayer().sendMessage(lang.getMessage("mod.items.restrictions.no-drop"));
            }
        }
    }

    private String getClickType(Action action) {
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) return "RIGHT_CLICK";
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) return "LEFT_CLICK";
        return null;
    }
}
