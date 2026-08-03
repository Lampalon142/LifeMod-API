package fr.lampalon.lifemod.platform.bukkit.managers;

import com.github.retrooper.packetevents.PacketEvents;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.listeners.NoClipPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class NoClipManager {

    private static final float DEFAULT_FLY_SPEED = 0.1f;

    private final LifeMod plugin;
    private final Map<UUID, FlightState> states = new HashMap<>();
    private int visibilityTaskId = -1;

    public NoClipManager(LifeMod plugin) {
        this.plugin = plugin;
        PacketEvents.getAPI().getEventManager().registerListener(new NoClipPacketListener(this));
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

        states.put(player.getUniqueId(), new FlightState(
                player.getGameMode(), player.isFlying(), player.getAllowFlight(), player.getFlySpeed()));

        // Server-side SPECTATOR gives collision-free flight; the packet listener
        // spoofs the client into CREATIVE so interactions behave natively.
        player.setGameMode(GameMode.SPECTATOR);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(flySpeed());

        startVisibilityTask();

        ILangService lang = ServiceRegistry.get(ILangService.class);
        player.sendMessage(lang.getMessage("commands.noclip.activate"));
    }

    public void disableNoClip(Player player) {
        if (player == null) return;
        FlightState state = states.remove(player.getUniqueId());

        player.setGameMode(state != null ? state.originalMode : GameMode.SURVIVAL);

        if (state != null) {
            player.setFlying(state.wasFlying && state.allowFlight);
            player.setAllowFlight(state.allowFlight);
            player.setFlySpeed(state.flySpeed);
        }

        stopVisibilityIfEmpty();

        ILangService lang = ServiceRegistry.get(ILangService.class);
        player.sendMessage(lang.getMessage("commands.noclip.deactivate"));
    }

    public void cleanupQuit(UUID uuid) {
        if (states.remove(uuid) != null) {
            stopVisibilityIfEmpty();
        }
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(states.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                disableNoClip(p);
            } else {
                states.remove(uuid);
            }
        }
        if (visibilityTaskId != -1) {
            Bukkit.getScheduler().cancelTask(visibilityTaskId);
            visibilityTaskId = -1;
        }
    }

    private void startVisibilityTask() {
        if (visibilityTaskId != -1) return;
        long interval = visibilityInterval();
        visibilityTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (states.isEmpty()) {
                Bukkit.getScheduler().cancelTask(visibilityTaskId);
                visibilityTaskId = -1;
                return;
            }
            for (UUID uuid : new HashSet<>(states.keySet())) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;
                boolean isVanished = plugin.getVanishService().isVanished(uuid);
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (other.equals(p)) continue;
                    if (isVanished && !other.hasPermission("lifemod.vanish.see")) continue;
                    other.showPlayer(plugin, p);
                }
            }
        }, interval, interval);
    }

    private void stopVisibilityIfEmpty() {
        if (states.isEmpty() && visibilityTaskId != -1) {
            Bukkit.getScheduler().cancelTask(visibilityTaskId);
            visibilityTaskId = -1;
        }
    }

    public boolean isNoClip(UUID uuid) {
        return states.containsKey(uuid);
    }

    public GameMode getOriginalGameMode(UUID uuid) {
        FlightState state = states.get(uuid);
        return state != null ? state.originalMode : GameMode.SURVIVAL;
    }

    public float flySpeed() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        float speed = cfg != null ? (float) cfg.getDouble("modules.noclip.speed", 0.3) : 0.3f;
        return Math.max(0.0f, Math.min(speed, 1.0f));
    }

    public double reach() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        return cfg != null ? cfg.getDouble("modules.noclip.reach", 7) : 7;
    }

    private long visibilityInterval() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        long interval = cfg != null ? cfg.getLong("modules.noclip.visibility-refresh-interval", 20) : 20L;
        return Math.max(1L, interval);
    }

    public LifeMod getPlugin() {
        return plugin;
    }

    private record FlightState(GameMode originalMode, boolean wasFlying, boolean allowFlight, float flySpeed) {
    }
}
