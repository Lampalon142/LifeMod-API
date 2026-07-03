package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TraceItemListener extends LogBaseListener {

    private static final NamespacedKey TRACE_KEY = new NamespacedKey(LifeMod.getInstance(), "trace_id");
    private static final String ACTION_CRAFT = "CRAFT";
    private static final String ACTION_PICKUP = "PICKUP";
    private static final String ACTION_DROP = "DROP";
    private static final String ACTION_CONSUME = "CONSUME";
    private static final String ACTION_DESPAWN = "DESPAWN";
    private static final String ACTION_RETRIEVE = "RETRIEVE";
    private static final String ACTION_STORE = "STORE";
    private static final String ACTION_SMELT = "SMELT";
    private static final String ACTION_ENCHANT = "ENCHANT";
    private static final String ACTION_MOB_DROP = "MOB_DROP";
    private static final String ACTION_FISHING = "FISHING";
    private static final String ACTION_DESTROYED = "DESTROYED";
    private static final String ACTION_SPAWNED = "SPAWNED";

    public TraceItemListener() {
        // Tâche périodique : détruit les items dans la lave/fire/void/cactus
        org.bukkit.Bukkit.getScheduler().runTaskTimer(
                LifeMod.getInstance(), this::scanForDestroyedItems, 100L, 100L);
    }
    private static final Set<InventoryType> STORAGE_TYPES = new HashSet<>(Arrays.asList(
            InventoryType.CHEST, InventoryType.DISPENSER, InventoryType.DROPPER,
            InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER,
            InventoryType.BREWING, InventoryType.HOPPER, InventoryType.BARREL,
            InventoryType.SHULKER_BOX, InventoryType.ENDER_CHEST
    ));

    public static String getOrAssignTraceId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(TRACE_KEY, PersistentDataType.STRING);
        if (id == null) {
            id = UUID.randomUUID().toString();
            pdc.set(TRACE_KEY, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return id;
    }

    public static String readTraceId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(TRACE_KEY, PersistentDataType.STRING);
    }

    private void logTrace(String traceId, ItemStack item, String action, Player player,
                          String world, int x, int y, int z) {
        if (traceId == null || item == null || item.getType().isAir()) return;
        LogEntry.Builder builder = LogEntry.builder()
                .type(LogType.ITEM_TRACE)
                .targetUuid(UUID.fromString(traceId))
                .actionData("{\"action\":\"" + action + "\",\"item_type\":\"" + item.getType().name() + "\"}")
                .world(world).x(x).y(y).z(z)
                .serverName(serverName())
                .now();
        if (player != null) {
            builder.playerUuid(player.getUniqueId()).playerName(player.getName());
        }
        logAsync(builder.build());
    }

    // ─── CRAFT ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;
        String traceId = getOrAssignTraceId(result);
        logTrace(traceId, result, ACTION_CRAFT, p,
                p.getLocation().getWorld().getName(),
                p.getLocation().getBlockX(),
                p.getLocation().getBlockY(),
                p.getLocation().getBlockZ());
    }

    // ─── PICKUP ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        if (item.getType().isAir()) return;
        String traceId = getOrAssignTraceId(item);
        logTrace(traceId, item, ACTION_PICKUP, p,
                p.getLocation().getWorld().getName(),
                p.getLocation().getBlockX(),
                p.getLocation().getBlockY(),
                p.getLocation().getBlockZ());
    }

    // ─── DROP ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType().isAir()) return;
        String traceId = getOrAssignTraceId(item);
        logTrace(traceId, item, ACTION_DROP, p,
                p.getLocation().getWorld().getName(),
                p.getLocation().getBlockX(),
                p.getLocation().getBlockY(),
                p.getLocation().getBlockZ());
    }

    // ─── CONSUME ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item.getType().isAir()) return;
        String traceId = readTraceId(item);
        logTrace(traceId, item, ACTION_CONSUME, p,
                p.getLocation().getWorld().getName(),
                p.getLocation().getBlockX(),
                p.getLocation().getBlockY(),
                p.getLocation().getBlockZ());
    }

    // ─── DESPAWN ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDespawn(ItemDespawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        if (item.getType().isAir()) return;
        String traceId = readTraceId(item);
        logTrace(traceId, item, ACTION_DESPAWN, null,
                event.getLocation().getWorld().getName(),
                event.getLocation().getBlockX(),
                event.getLocation().getBlockY(),
                event.getLocation().getBlockZ());
    }

    // ─── INVENTORY SCAN À L'OUVERTURE (attrape-tout) ─────────────────
    // Tout item ouvert dans un inventaire reçoit un UUID s'il en manque un.
    // Cela couvre : /give, shop plugins, creative, etc.

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        Inventory top = event.getView().getTopInventory();
        Inventory bottom = event.getView().getBottomInventory();
        if (top == null || bottom == null) return;
        if (top.equals(bottom)) return;

        Inventory clicked = event.getClickedInventory();
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) return;

        boolean inTop = clicked != null && clicked.equals(top);

        switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY:
                if (inTop) {
                    // Prendre du coffre → inventaire joueur
                    String traceId = readTraceId(current);
                    logTrace(traceId, current, ACTION_RETRIEVE, p,
                            p.getLocation().getWorld().getName(),
                            p.getLocation().getBlockX(),
                            p.getLocation().getBlockY(),
                            p.getLocation().getBlockZ());
                } else {
                    // Mettre dans le coffre depuis l'inventaire joueur
                    String traceId = readTraceId(current);
                    logTrace(traceId, current, ACTION_STORE, p,
                            p.getLocation().getWorld().getName(),
                            p.getLocation().getBlockX(),
                            p.getLocation().getBlockY(),
                            p.getLocation().getBlockZ());
                }
                break;

            case PICKUP_ALL:
            case PICKUP_HALF:
            case PICKUP_ONE:
                if (inTop && top.getType() != InventoryType.PLAYER) {
                    String traceId = readTraceId(current);
                    logTrace(traceId, current, ACTION_RETRIEVE, p,
                            p.getLocation().getWorld().getName(),
                            p.getLocation().getBlockX(),
                            p.getLocation().getBlockY(),
                            p.getLocation().getBlockZ());
                }
                break;

            case PLACE_ALL:
            case PLACE_ONE:
                if (inTop) {
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && !cursor.getType().isAir()) {
                        String traceId = readTraceId(cursor);
                        logTrace(traceId, cursor, ACTION_STORE, p,
                                p.getLocation().getWorld().getName(),
                                p.getLocation().getBlockX(),
                                p.getLocation().getBlockY(),
                                p.getLocation().getBlockZ());
                    }
                }
                break;

            case HOTBAR_MOVE_AND_READD:
            case HOTBAR_SWAP:
                if (inTop) {
                    String traceId = readTraceId(current);
                    logTrace(traceId, current, ACTION_RETRIEVE, p,
                            p.getLocation().getWorld().getName(),
                            p.getLocation().getBlockX(),
                            p.getLocation().getBlockY(),
                            p.getLocation().getBlockZ());
                }
                break;

            case SWAP_WITH_CURSOR:
                if (inTop) {
                    String traceId = readTraceId(current);
                    logTrace(traceId, current, ACTION_RETRIEVE, p,
                            p.getLocation().getWorld().getName(),
                            p.getLocation().getBlockX(),
                            p.getLocation().getBlockY(),
                            p.getLocation().getBlockZ());
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && !cursor.getType().isAir()) {
                        String traceCId = readTraceId(cursor);
                        logTrace(traceCId, cursor, ACTION_STORE, p,
                                p.getLocation().getWorld().getName(),
                                p.getLocation().getBlockX(),
                                p.getLocation().getBlockY(),
                                p.getLocation().getBlockZ());
                    }
                }
                break;
        }
    }

    // ─── FOURNAISE (transfère l'UUID de l'input vers le résultat) ─────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        ItemStack result = event.getResult();
        if (source.getType().isAir() || result.getType().isAir()) return;
        String sourceTraceId = readTraceId(source);
        if (sourceTraceId == null) return;
        result = result.clone();
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(TRACE_KEY, PersistentDataType.STRING, sourceTraceId);
        result.setItemMeta(meta);
        event.setResult(result);
        logTrace(sourceTraceId, result, ACTION_SMELT, null,
                event.getBlock().getWorld().getName(),
                event.getBlock().getX(),
                event.getBlock().getY(),
                event.getBlock().getZ());
    }

    // ─── ENCHANTEMENT ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player p = event.getEnchanter();
        ItemStack item = event.getItem();
        if (item.getType().isAir()) return;
        String traceId = readTraceId(item);
        logTrace(traceId, item, ACTION_ENCHANT, p,
                p.getLocation().getWorld().getName(),
                p.getLocation().getBlockX(),
                p.getLocation().getBlockY(),
                p.getLocation().getBlockZ());
    }

    // ─── ENCLUME (transfère l'UUID de l'item de gauche vers le résultat) ─

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir()) return;
        ItemStack left = event.getInventory().getItem(0);
        if (left == null || left.getType().isAir()) return;
        String leftTraceId = readTraceId(left);
        if (leftTraceId == null) return;
        if (leftTraceId.equals(readTraceId(result))) return;
        result = result.clone();
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(TRACE_KEY, PersistentDataType.STRING, leftTraceId);
        result.setItemMeta(meta);
        event.setResult(result);
    }

    // ─── MOB DROPS ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        String world = event.getEntity().getWorld().getName();
        int x = event.getEntity().getLocation().getBlockX();
        int y = event.getEntity().getLocation().getBlockY();
        int z = event.getEntity().getLocation().getBlockZ();
        for (ItemStack drop : event.getDrops()) {
            if (drop.getType().isAir()) continue;
            String traceId = getOrAssignTraceId(drop);
            if (traceId != null) {
                logAsync(LogEntry.builder()
                        .type(LogType.ITEM_TRACE)
                        .playerUuid(killer != null ? killer.getUniqueId() : null)
                        .playerName(killer != null ? killer.getName() : "?" + event.getEntity().getType().name())
                        .targetUuid(UUID.fromString(traceId))
                        .actionData("{\"action\":\"" + ACTION_MOB_DROP + "\",\"item_type\":\"" + drop.getType().name() + "\"}")
                        .world(world).x(x).y(y).z(z)
                        .serverName(serverName())
                        .now()
                        .build());
            }
        }
    }

    // ─── PÊCHE ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item)) return;
        Player p = event.getPlayer();
        ItemStack item = ((Item) event.getCaught()).getItemStack();
        if (item.getType().isAir()) return;
        String traceId = getOrAssignTraceId(item);
        logTrace(traceId, item, ACTION_FISHING, p,
                p.getLocation().getWorld().getName(),
                p.getLocation().getBlockX(),
                p.getLocation().getBlockY(),
                p.getLocation().getBlockZ());
    }

    // ─── SCAN À L'OUVERTURE D'INVENTAIRE (attrape-tout) ─────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        scanInventory(event.getInventory(), p, event.getInventory().getLocation());
    }

    private void scanInventory(Inventory inv, Player player, Location containerLoc) {
        String worldName = containerLoc != null
                ? containerLoc.getWorld().getName()
                : player.getLocation().getWorld().getName();
        int bx = containerLoc != null ? containerLoc.getBlockX() : player.getLocation().getBlockX();
        int by = containerLoc != null ? containerLoc.getBlockY() : player.getLocation().getBlockY();
        int bz = containerLoc != null ? containerLoc.getBlockZ() : player.getLocation().getBlockZ();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            if (readTraceId(item) != null) continue;
            String traceId = getOrAssignTraceId(item);
            logTrace(traceId, item, ACTION_SPAWNED, player, worldName, bx, by, bz);
        }
    }

    // ─── SCAN PÉRIODIQUE : destruction par lave / fire / void / cactus ──
    // Exécuté toutes les 5 secondes (100 ticks) sur tous les mondes chargés.

    private void scanForDestroyedItems() {
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (!(entity instanceof Item itemEntity)) continue;
                ItemStack item = itemEntity.getItemStack();
                String traceId = readTraceId(item);
                if (traceId == null) continue;
                org.bukkit.Location loc = itemEntity.getLocation();
                Material block = loc.getBlock().getType();
                boolean destroyed = block == Material.LAVA
                        || block == Material.FIRE
                        || block == Material.SOUL_FIRE
                        || block == Material.CACTUS
                        || loc.getY() < world.getMinHeight();
                if (!destroyed) continue;
                logTrace(traceId, item, ACTION_DESTROYED, null,
                        world.getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                itemEntity.remove();
            }
        }
    }
}
