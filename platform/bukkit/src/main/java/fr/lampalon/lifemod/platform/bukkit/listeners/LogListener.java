package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILogService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LogListener implements Listener {

    private final ILogService logService;
    private final IConfigurationService config;
    private final Map<UUID, Long> containerOpenSince = new HashMap<>();
    private final Map<UUID, Inventory> openContainers = new HashMap<>();

    private static final String[] IGNORED_CMDS = {
        "/shop", "/warp", "/spawn", "/tpa", "/tpahere",
        "/bal", "/balance", "/pay", "/msg", "/r", "/reply"
    };

    public LogListener() {
        this.logService = ServiceRegistry.get(ILogService.class);
        this.config = ServiceRegistry.get(IConfigurationService.class);
    }

    private boolean enabled(String path) {
        return config.getBoolean("logs." + path, true);
    }

    private boolean isIgnoredCommand(String cmd) {
        String lower = cmd.toLowerCase();
        for (String ignored : IGNORED_CMDS) {
            if (lower.startsWith(ignored)) return true;
        }
        return false;
    }

    private void logAsync(LogEntry entry) {
        if (logService != null) logService.log(entry);
    }

    private static String locData(Location loc) {
        if (loc == null) return null;
        return loc.getWorld() != null ? loc.getWorld().getName() : null;
    }

    // ── Connexion / Déconnexion ──

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled("log-connection")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.LOGIN_SUCCESS).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"ip\":\"" + p.getAddress() + "\"}").serverName(serverName())
            .now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled("log-connection")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.QUIT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .serverName(serverName()).now().build());
        containerOpenSince.remove(p.getUniqueId());
        openContainers.remove(p.getUniqueId());
    }

    // ── Chat ──

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled("log-chat")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.CHAT_MESSAGE).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"m\":\"" + jsonEscape(event.getMessage()) + "\"}")
            .serverName(serverName()).now().build());
    }

    // ── Commandes ──

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled("log-commands")) return;
        String cmd = event.getMessage();
        if (isIgnoredCommand(cmd)) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.COMMAND_EXECUTED).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"c\":\"" + jsonEscape(cmd) + "\"}")
            .serverName(serverName()).now().build());
    }

    // ── Mort / PvP ──

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!enabled("log-death")) return;
        Player p = event.getEntity();
        LogType type;
        String data;
        if (p.getKiller() != null) {
            type = LogType.DEATH_PLAYER;
            data = "{\"k\":\"" + p.getKiller().getName() + "\",\"w\":\"" + p.getKiller().getInventory().getItemInMainHand().getType().name() + "\"}";
        } else if (p.getLastDamageCause() != null) {
            switch (p.getLastDamageCause().getCause()) {
                case FALL: case FLY_INTO_WALL: type = LogType.DEATH_ENVIRONMENT; data = "{\"c\":\"fall\"}"; break;
                case DROWNING: type = LogType.DEATH_ENVIRONMENT; data = "{\"c\":\"drown\"}"; break;
                case LAVA: case FIRE: case FIRE_TICK: type = LogType.DEATH_ENVIRONMENT; data = "{\"c\":\"fire\"}"; break;
                case SUFFOCATION: type = LogType.DEATH_ENVIRONMENT; data = "{\"c\":\"suffocate\"}"; break;
                case ENTITY_ATTACK: case ENTITY_SWEEP_ATTACK:
                    type = LogType.DEATH_MOB;
                    data = p.getLastDamageCause().getEntity() != null
                        ? "{\"m\":\"" + p.getLastDamageCause().getEntity().getType().name() + "\"}"
                        : "{}";
                    break;
                default: type = LogType.DEATH_ENVIRONMENT;
                    data = "{\"c\":\"" + p.getLastDamageCause().getCause().name() + "\"}";
            }
        } else {
            type = LogType.DEATH_OTHER;
            data = "{}";
        }
        Location loc = p.getLocation();
        logAsync(LogEntry.builder()
            .type(type).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData(data).world(locData(loc)).x(loc.getBlockX()).y(loc.getBlockY()).z(loc.getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKillMob(EntityDeathEvent event) {
        if (!enabled("log-death")) return;
        if (event.getEntity() instanceof Player) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        Location loc = event.getEntity().getLocation();
        logAsync(LogEntry.builder()
            .type(LogType.KILL_MOB).playerUuid(killer.getUniqueId()).playerName(killer.getName())
            .targetName(event.getEntity().getType().name())
            .actionData("{\"m\":\"" + event.getEntity().getType().name() + "\"}")
            .world(locData(loc)).x(loc.getBlockX()).y(loc.getBlockY()).z(loc.getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!enabled("log-death")) return;
        Player p = event.getPlayer();
        Location loc = event.getRespawnLocation();
        logAsync(LogEntry.builder()
            .type(LogType.RESPAWN).playerUuid(p.getUniqueId()).playerName(p.getName())
            .world(locData(loc)).x(loc.getBlockX()).y(loc.getBlockY()).z(loc.getBlockZ())
            .serverName(serverName()).now().build());
    }

    // ── Blocs ──

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

    // ── Conteneurs ──

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
            // Determine direction: if clicked slot is in top inventory → player took from container
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

        // Shift-click: items move between inventories
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            if (current != null && current.getType() != Material.AIR) {
                // If clicked in top → taking out of container
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

    // ── Items ──

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!enabled("log-item")) return;
        Player p = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        logAsync(LogEntry.builder()
            .type(LogType.ITEM_DROP).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + item.getType().name() + "\",\"a\":" + item.getAmount() + "}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!enabled("log-item")) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        logAsync(LogEntry.builder()
            .type(LogType.ITEM_PICKUP).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + item.getType().name() + "\",\"a\":" + item.getAmount() + "}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!enabled("log-item")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.ITEM_CONSUME).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + event.getItem().getType().name() + "\"}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!enabled("log-item")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();
        logAsync(LogEntry.builder()
            .type(LogType.CRAFT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + result.getType().name() + "\",\"a\":" + result.getAmount() + "}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!enabled("log-item")) return;
        Player p = event.getEnchanter();
        StringBuilder enchants = new StringBuilder();
        event.getEnchantsToAdd().forEach((e, lvl) -> {
            if (enchants.length() > 0) enchants.append(",");
            enchants.append(e.getKey().getKey()).append(":").append(lvl);
        });
        logAsync(LogEntry.builder()
            .type(LogType.ENCHANT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + event.getItem().getType().name() + "\",\"e\":\"" + enchants + "\"}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    // ── Entités ──

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!enabled("log-entity")) return;
        if (!(event.getOwner() instanceof Player)) return;
        Player p = (Player) event.getOwner();
        logAsync(LogEntry.builder()
            .type(LogType.ENTITY_TAME).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"e\":\"" + event.getEntity().getType().name() + "\"}")
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (!enabled("log-entity")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.SHEEP_SHEAR).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"e\":\"" + event.getEntity().getType().name() + "\"}")
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!enabled("log-entity")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.ENTITY_INTERACT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .targetName(event.getRightClicked().getType().name())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!enabled("log-entity")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.FISHING).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"s\":\"" + event.getState().name() + "\"}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    // ── Véhicules ──

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!enabled("log-entity")) return;
        if (!(event.getEntered() instanceof Player)) return;
        Player p = (Player) event.getEntered();
        logAsync(LogEntry.builder()
            .type(LogType.VEHICLE_ENTER).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"v\":\"" + event.getVehicle().getType().name() + "\"}")
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!enabled("log-entity")) return;
        if (!(event.getExited() instanceof Player)) return;
        Player p = (Player) event.getExited();
        logAsync(LogEntry.builder()
            .type(LogType.VEHICLE_EXIT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"v\":\"" + event.getVehicle().getType().name() + "\"}")
            .serverName(serverName()).now().build());
    }

    // ── Mouvement ──

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!enabled("log-teleport")) return;
        Player p = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        logAsync(LogEntry.builder()
            .type(LogType.TELEPORT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"c\":\"" + event.getCause().name() + "\",\"fx\":" + from.getBlockX() + ",\"fy\":" + from.getBlockY() + ",\"fz\":" + from.getBlockZ() + ",\"fw\":\"" + locData(from) + "\"}")
            .world(locData(to)).x(to.getBlockX()).y(to.getBlockY()).z(to.getBlockZ())
            .serverName(serverName()).now().build());
    }

    // ── Monde ──

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!enabled("log-world")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.GAMEMODE_CHANGE).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"f\":\"" + p.getGameMode().name() + "\",\"t\":\"" + event.getNewGameMode().name() + "\"}")
            .serverName(serverName()).now().build());
    }

    // ── Helpers ──

    private String serverName() {
        return config.getString("server.name", "unknown");
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
