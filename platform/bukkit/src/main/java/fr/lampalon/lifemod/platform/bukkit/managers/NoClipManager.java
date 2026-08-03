package fr.lampalon.lifemod.platform.bukkit.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * NoClip implemented as CREATIVE flight with a server-side air mask.
 * Solid blocks around the player are resent to his client as AIR, so the
 * client does not collide with them and the moderator can fly through every
 * wall while keeping the full creative interface (hand, abilities).
 */
public class NoClipManager {

    private static final int AIR_ID = airId();

    private final LifeMod plugin;
    private final Map<UUID, SavedState> states = new HashMap<>();
    private final Map<UUID, Set<Pos>> masked = new HashMap<>();
    private final Map<UUID, GameMode> pendingRestore = new HashMap<>();
    private int taskId = -1;

    public NoClipManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    public void toggleNoClip(Player player) {
        if (player == null) return;
        if (isNoClip(player.getUniqueId())) {
            disableNoClip(player);
        } else {
            enableNoClip(player);
        }
    }

    public void enableNoClip(Player player) {
        if (player == null || isNoClip(player.getUniqueId())) return;

        states.put(player.getUniqueId(), new SavedState(
                player.getGameMode(), player.isFlying(), player.getAllowFlight(), player.getFlySpeed()));

        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(flySpeed());

        startMaskTask();
        send("commands.noclip.activate", player);
    }

    public void disableNoClip(Player player) {
        if (player == null) return;
        SavedState state = states.remove(player.getUniqueId());

        restoreMask(player.getUniqueId());
        player.setAllowFlight(false);

        if (state != null) {
            player.setGameMode(state.originalMode);
            player.setFlying(state.wasFlying && state.allowFlight);
            player.setAllowFlight(state.allowFlight);
            player.setFlySpeed(state.flySpeed);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            player.setFlying(false);
            player.setFlySpeed(0.2f);
        }

        unstick(player);
        stopMaskIfEmpty();
        send("commands.noclip.deactivate", player);
    }

    public void handleQuit(UUID uuid) {
        SavedState state = states.remove(uuid);
        if (state != null) {
            pendingRestore.put(uuid, state.originalMode);
        }
        restoreMask(uuid);
        stopMaskIfEmpty();
    }

    public void handleJoin(Player player) {
        GameMode mode = pendingRestore.remove(player.getUniqueId());
        if (mode != null) {
            player.setGameMode(mode);
            player.setFlying(false);
            send("commands.noclip.deactivate", player);
        }
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(states.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                disableNoClip(p);
            } else {
                handleQuit(uuid);
            }
        }
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public boolean isNoClip(UUID uuid) {
        return states.containsKey(uuid);
    }

    public float flySpeed() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        float speed = cfg != null ? (float) cfg.getDouble("modules.noclip.speed", 0.3) : 0.3f;
        return Math.max(0.0f, Math.min(speed, 1.0f));
    }

    private int maskRadius() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        return cfg != null ? cfg.getInt("modules.noclip.radius", 3) : 3;
    }

    private void startMaskTask() {
        if (taskId != -1) return;
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickMask, 0L, 1L).getTaskId();
    }

    private void tickMask() {
        if (masked.isEmpty()) {
            if (taskId != -1) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskId = -1;
            }
            return;
        }
        for (UUID uuid : new HashSet<>(masked.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                masked.remove(uuid);
                continue;
            }
            refreshMask(p, uuid);
        }
    }

    private void refreshMask(Player player, UUID uuid) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        int radius = maskRadius();

        Set<Pos> current = new HashSet<>();
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (!blockCollides(b)) continue;
                    current.add(new Pos(x, y, z));
                }
            }
        }

        Set<Pos> previous = masked.getOrDefault(uuid, Set.of());

        for (Pos pos : current) {
            if (!previous.contains(pos)) {
                sendBlock(player, pos, AIR_ID);
            }
        }
        for (Pos pos : previous) {
            if (!current.contains(pos)) {
                sendBlock(player, pos, toId(world, pos));
            }
        }

        masked.put(uuid, current);
    }

    private void restoreMask(UUID uuid) {
        Set<Pos> prev = masked.remove(uuid);
        if (prev == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || player.getWorld() == null) return;
        World world = player.getWorld();
        for (Pos pos : prev) {
            sendBlock(player, pos, toId(world, pos));
        }
    }

    private void stopMaskIfEmpty() {
        if (masked.isEmpty() && taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void send(String key, Player player) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        if (lang != null) {
            player.sendMessage(lang.getMessage(key));
        }
    }

    private boolean blockCollides(Block block) {
        return !block.isPassable();
    }

    private static void sendBlock(Player player, Pos pos, int blockId) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerBlockChange(new Vector3i(pos.x, pos.y, pos.z), blockId));
    }

    private static int toId(World world, Pos pos) {
        return SpigotConversionUtil.fromBukkitBlockData(
                world.getBlockAt(pos.x, pos.y, pos.z).getBlockData()).getGlobalId();
    }

    private static int airId() {
        try {
            return SpigotConversionUtil.fromBukkitBlockData(
                    Bukkit.createBlockData(Material.AIR)).getGlobalId();
        } catch (Exception e) {
            return 0;
        }
    }

    private void unstick(Player player) {
        World world = player.getWorld();
        if (world == null) return;
        Location loc = player.getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        if (world.getBlockAt(x, y, z).isPassable()) return;

        for (int dy = 1; dy <= 64; dy++) {
            if (world.getBlockAt(x, y + dy, z).isPassable()
                    && world.getBlockAt(x, y + dy + 1, z).isPassable()) {
                player.teleport(new Location(world, x + 0.5, y + dy, z + 0.5, loc.getYaw(), loc.getPitch()));
                return;
            }
        }
        for (int dy = -1; dy >= -64; dy--) {
            if (world.getBlockAt(x, y + dy, z).isPassable()
                    && world.getBlockAt(x, y + dy + 1, z).isPassable()) {
                player.teleport(new Location(world, x + 0.5, y + dy, z + 0.5, loc.getYaw(), loc.getPitch()));
                return;
            }
        }
    }

    public LifeMod getPlugin() {
        return plugin;
    }

    private record SavedState(GameMode originalMode, boolean wasFlying, boolean allowFlight, float flySpeed) {
    }

    private record Pos(int x, int y, int z) {
    }
}