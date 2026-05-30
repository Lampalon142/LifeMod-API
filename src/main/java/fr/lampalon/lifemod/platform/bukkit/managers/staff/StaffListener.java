package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.BukkitPlatform;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
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
    private final DebugManager debug;
    
    private final Map<UUID, Long> lastEntityInteractTick = new HashMap<>();

    public void handleQuit(UUID playerId) {
        lastEntityInteractTick.remove(playerId);
        debug.log("staff", "StaffListener handleQuit: " + playerId);
    }

    public StaffListener(StaffModeManager staffModeManager, StaffItemManager staffItemManager, StaffActionManager staffActionManager) {
        this.staffModeManager = staffModeManager;
        this.staffItemManager = staffItemManager;
        this.staffActionManager = staffActionManager;
        this.debug = LifeMod.getInstance().getDebugManager();
        debug.log("staff", "StaffListener initialized");
    }

    private boolean canEntityInteract(Player player) {
        long currentTick = Bukkit.getServer().getWorlds().get(0).getFullTime();
        Long lastTick = lastEntityInteractTick.get(player.getUniqueId());
        if (lastTick != null && lastTick == currentTick) {
            debug.log("staff", "canEntityInteract BLOCKED for " + player.getName() + " tick=" + currentTick);
            return false;
        }
        lastEntityInteractTick.put(player.getUniqueId(), currentTick);
        debug.log("staff", "canEntityInteract ALLOWED for " + player.getName() + " tick=" + currentTick);
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        debug.log("staff", "onInteract called for " + player.getName() + " action=" + event.getAction());

        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                debug.log("staff", "onInteract: OFF_HAND, skipping");
                return;
            }
        } catch (NoSuchMethodError ignored) {}

        if (!staffModeManager.isMod(player)) {
            debug.log("staff", "onInteract: " + player.getName() + " is not in mod mode, skipping");
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            debug.log("staff", "onInteract: RIGHT_CLICK_AIR skipped (entity handling)");
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            debug.log("staff", "onInteract: item is null");
            return;
        }

        debug.log("staff", "onInteract: item=" + item.getType() + " hasItemMeta=" + item.hasItemMeta());

        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) {
            debug.log("staff", "onInteract: staffItem not found for " + item.getType());
            return;
        }

        debug.log("staff", "onInteract: found staffItem key=" + staffItem.getKey());
        event.setCancelled(true);

        String clickType = getClickType(event.getAction());
        debug.log("staff", "onInteract: clickType=" + clickType);
        if (clickType == null) return;

        handleStaffAction(player, staffItem, clickType, event, null, null);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity targetEntity = event.getRightClicked();
        debug.log("staff", "onInteractEntity called for " + player.getName() + " target=" + targetEntity.getType() + (targetEntity instanceof Player ? " (" + ((Player) targetEntity).getName() + ")" : ""));

        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                debug.log("staff", "onInteractEntity: OFF_HAND, skipping");
                return;
            }
        } catch (NoSuchMethodError ignored) {}

        if (!staffModeManager.isMod(player)) {
            debug.log("staff", "onInteractEntity: " + player.getName() + " is not in mod mode, skipping");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        debug.log("staff", "onInteractEntity: item=" + (item == null ? "null" : item.getType().name()) + " hasItemMeta=" + (item != null && item.hasItemMeta()));

        if (item == null) return;

        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) {
            debug.log("staff", "onInteractEntity: staffItem not found");
            return;
        }

        debug.log("staff", "onInteractEntity: found staffItem key=" + staffItem.getKey());

        event.setCancelled(true);

        if (!canEntityInteract(player)) {
            debug.log("staff", "onInteractEntity: blocked by canEntityInteract");
            return;
        }

        handleStaffAction(player, staffItem, "RIGHT_CLICK", null, event, event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        debug.log("staff", "onEntityDamage called for " + player.getName() + " target=" + event.getEntity().getType());

        if (!staffModeManager.isMod(player)) {
            debug.log("staff", "onEntityDamage: " + player.getName() + " not in mod mode");
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) {
            debug.log("staff", "onEntityDamage: item null");
            return;
        }
        StaffItem staffItem = staffItemManager.getStaffItem(item);
        if (staffItem == null) {
            debug.log("staff", "onEntityDamage: staffItem not found for " + item.getType());
            return;
        }

        debug.log("staff", "onEntityDamage: staffItem key=" + staffItem.getKey());

        if (!canEntityInteract(player)) {
            debug.log("staff", "onEntityDamage: blocked by canEntityInteract");
            return;
        }

        List<String> scripts = staffItem.getScripts("LEFT_CLICK");
        debug.log("staff", "onEntityDamage: LEFT_CLICK scripts=" + (scripts == null ? "null" : String.valueOf(scripts.size())));
        if (scripts != null && !scripts.isEmpty()) {
            event.setCancelled(true);
            debug.log("staff", "onEntityDamage: dispatching LEFT_CLICK action");
            handleStaffAction(player, staffItem, "LEFT_CLICK", null, null, event.getEntity());
        }
    }

    private void handleStaffAction(Player player, StaffItem staffItem, String clickType, PlayerInteractEvent interactEvent, PlayerInteractEntityEvent entityInteractEvent, Entity target) {
        debug.log("staff", "handleStaffAction: player=" + player.getName() + " itemKey=" + staffItem.getKey() + " clickType=" + clickType + " hasEntity=" + (entityInteractEvent != null) + " hasInteract=" + (interactEvent != null) + " target=" + (target == null ? "null" : target.getType().name()));
        List<String> scripts = staffItem.getScripts(clickType);
        if (scripts == null || scripts.isEmpty()) {
            debug.log("staff", "handleStaffAction: no scripts for clickType=" + clickType);
            return;
        }
        debug.log("staff", "handleStaffAction: " + scripts.size() + " scripts to execute");

        for (String script : scripts) {
            executeScript(player, script, interactEvent, entityInteractEvent, target);
        }
    }

    private void executeScript(Player player, String script, PlayerInteractEvent interactEvent, PlayerInteractEntityEvent entityInteractEvent, Entity target) {
        String upper = script.toUpperCase();
        debug.log("staff", "executeScript: raw='" + script + "' upper='" + upper + "'");

        if (upper.startsWith("[PLAYER]")) {
            String cmd = script.substring(8).trim().replace("%player%", player.getName()).replace("%player_name%", player.getName());
            debug.log("staff", "executeScript: [PLAYER] cmd=" + cmd);
            player.performCommand(cmd);
        } 
        else if (upper.startsWith("[CONSOLE]")) {
            String cmd = script.substring(9).trim().replace("%player%", player.getName()).replace("%player_name%", player.getName());
            if (target != null) {
                cmd = cmd.replace("%target%", target.getName());
            }
            debug.log("staff", "executeScript: [CONSOLE] cmd=" + cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        else if (upper.startsWith("[MESSAGE]")) {
            fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            String msg = lang.getMessage(script.substring(9).trim());
            debug.log("staff", "executeScript: [MESSAGE] key=" + script.substring(9).trim() + " resolved=" + msg);
            player.sendMessage(msg);
        }
        else if (upper.startsWith("[ACTIONBAR]")) {
            fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
            if (platform instanceof BukkitPlatform) {
                NMSProvider nms = ((BukkitPlatform) platform).getNmsProvider();
                if (nms != null) {
                    String msg = lang.getMessage(script.substring(11).trim());
                    debug.log("staff", "executeScript: [ACTIONBAR] key=" + script.substring(11).trim());
                    nms.sendActionBar(player, msg);
                } else {
                    debug.log("staff", "executeScript: [ACTIONBAR] nms is null");
                }
            } else {
                debug.log("staff", "executeScript: [ACTIONBAR] platform is not BukkitPlatform");
            }
        }
        else if (upper.startsWith("[SOUND]")) {
            try {
                String soundName = script.substring(7).trim().toUpperCase();
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                debug.log("staff", "executeScript: [SOUND] " + soundName);
                player.playSound(player.getLocation(), sound, 1f, 1f);
            } catch (Exception e) {
                debug.log("staff", "executeScript: [SOUND] error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (upper.startsWith("[NATIVE]")) {
            String actionName = script.substring(8).trim().toUpperCase();
            debug.log("staff", "executeScript: [NATIVE] actionName=" + actionName);
            StaffActionType actionType = StaffActionType.fromString(actionName);
            if (actionType != null) {
                debug.log("staff", "executeScript: [NATIVE] resolved to StaffActionType." + actionType);
                executeNativeAction(player, actionType, interactEvent, entityInteractEvent, target);
            } else {
                debug.log("staff", "executeScript: [NATIVE] UNRECOGNIZED actionType=" + actionName);
            }
        }
        else {
            debug.log("staff", "executeScript: bare action, trying StaffActionType.fromString");
            StaffActionType actionType = StaffActionType.fromString(script.trim());
            if (actionType != null) {
                debug.log("staff", "executeScript: bare action resolved to " + actionType);
                executeNativeAction(player, actionType, interactEvent, entityInteractEvent, target);
            } else {
                debug.log("staff", "executeScript: bare action UNRECOGNIZED: '" + script.trim() + "'");
            }
        }
    }

    private void executeNativeAction(Player player, StaffActionType actionType, PlayerInteractEvent interactEvent, PlayerInteractEntityEvent entityInteractEvent, Entity target) {
        IStaffAction action = staffActionManager.getAction(actionType);
        debug.log("staff", "executeNativeAction: player=" + player.getName() + " type=" + actionType + " action=" + (action == null ? "null" : action.getClass().getSimpleName()));
        if (action == null) {
            debug.log("staff", "executeNativeAction: NO ACTION REGISTERED for " + actionType);
            return;
        }

        if (entityInteractEvent != null) {
            debug.log("staff", "executeNativeAction: calling onInteractEntity for " + actionType);
            action.onInteractEntity(player, entityInteractEvent);
        } else if (interactEvent != null) {
            debug.log("staff", "executeNativeAction: calling onInteract for " + actionType);
            action.onInteract(player, interactEvent);
        } else if (target != null) {
            debug.log("staff", "executeNativeAction: calling onAttack for " + actionType + " target=" + target.getType().name());
            action.onAttack(player, target);
        } else {
            debug.log("staff", "executeNativeAction: NO EVENT/TARGET to dispatch");
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
                debug.log("staff", "onInventoryClick: blocked put of staff item into external inventory for " + player.getName());
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
                    debug.log("staff", "onInventoryClick: blocked shift-click of staff item for " + player.getName());
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
                debug.log("staff", "onDrop: blocked drop of staff item for " + event.getPlayer().getName());
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
