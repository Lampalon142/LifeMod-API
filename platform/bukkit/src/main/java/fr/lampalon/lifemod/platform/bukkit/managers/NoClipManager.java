package fr.lampalon.lifemod.platform.bukkit.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
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

/**
 * NoClip: the server puts the moderator in SPECTATOR (native, collision-free
 * flight through every block). A packet listener only rewrites the client-side
 * "Change Game State" gamemode back to the original value, so the HUD keeps
 * looking like CREATIVE/SURVIVAL while the client stays collision-free.
 */
public class NoClipManager {

    private final LifeMod plugin;
    private final Map<UUID, SavedState> states = new HashMap<>();
    private final Map<UUID, GameMode> pendingRestore = new HashMap<>();

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

        states.put(player.getUniqueId(), new SavedState(
                player.getGameMode(), player.isFlying(), player.getAllowFlight(), player.getFlySpeed()));

        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(flySpeed());

        send("commands.noclip.activate", player);
    }

    public void disableNoClip(Player player) {
        if (player == null) return;
        SavedState state = states.remove(player.getUniqueId());

        player.setGameMode(state != null ? state.originalMode : GameMode.SURVIVAL);

        if (state != null) {
            player.setFlying(state.wasFlying && state.allowFlight);
            player.setAllowFlight(state.allowFlight);
            player.setFlySpeed(state.flySpeed);
        }

        send("commands.noclip.deactivate", player);
    }

    public void handleQuit(UUID uuid) {
        SavedState state = states.remove(uuid);
        if (state != null) {
            pendingRestore.put(uuid, state.originalMode);
        }
    }

    public void handleJoin(Player player) {
        GameMode mode = pendingRestore.remove(player.getUniqueId());
        if (mode != null) {
            player.setGameMode(mode);
            player.setFlying(false);
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
    }

    public boolean isNoClip(UUID uuid) {
        return states.containsKey(uuid);
    }

    public GameMode getOriginalGameMode(UUID uuid) {
        SavedState state = states.get(uuid);
        return state != null ? state.originalMode : GameMode.SURVIVAL;
    }

    public float flySpeed() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        float speed = cfg != null ? (float) cfg.getDouble("modules.noclip.speed", 0.3) : 0.3f;
        return Math.max(0.0f, Math.min(speed, 1.0f));
    }

    private void send(String key, Player player) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        if (lang != null) {
            player.sendMessage(lang.getMessage(key));
        }
    }

    public LifeMod getPlugin() {
        return plugin;
    }

    private record SavedState(GameMode originalMode, boolean wasFlying, boolean allowFlight, float flySpeed) {
    }
}