package fr.lampalon.lifemod.platform.bukkit.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.listeners.NoClipPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

public class NoClipManager {

    private final LifeMod plugin;
    private final Map<UUID, GameMode> noclipPlayers = new HashMap<>();
    private int visibilityTaskId = -1;

    public NoClipManager(LifeMod plugin) {
        this.plugin = plugin;
        PacketEvents.getAPI().getEventManager()
                .registerListener(new NoClipPacketListener(this));
    }

    public void toggleNoClip(Player player) {
        if (isNoClip(player.getUniqueId())) {
            disableNoClip(player);
        } else {
            enableNoClip(player);
        }
    }

    public void enableNoClip(Player player) {
        GameMode original = player.getGameMode();
        noclipPlayers.put(player.getUniqueId(), original);

        player.setGameMode(GameMode.SPECTATOR);
        spoofClientGameMode(player, original);

        player.setFlySpeed(0.2f);
        player.setAllowFlight(true);
        player.setFlying(true);

        startVisibilityTask();

        ILangService lang = ServiceRegistry.get(ILangService.class);
        player.sendMessage(lang.getMessage("commands.noclip.activate"));

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("action", "enable");
            ph.capture("lifemod_noclip", props);
        }
    }

    public void disableNoClip(Player player) {
        GameMode original = noclipPlayers.remove(player.getUniqueId());

        player.setGameMode(original != null ? original : GameMode.SURVIVAL);
        spoofClientGameMode(player, original != null ? original : GameMode.SURVIVAL);
        player.setFlySpeed(0.1f);

        ILangService lang = ServiceRegistry.get(ILangService.class);
        player.sendMessage(lang.getMessage("commands.noclip.deactivate"));

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("action", "disable");
            ph.capture("lifemod_noclip", props);
        }
    }

    private void spoofClientGameMode(Player player, GameMode gameMode) {
        int value = switch (gameMode) {
            case SURVIVAL  -> 0;
            case CREATIVE  -> 1;
            case ADVENTURE -> 2;
            case SPECTATOR -> 3;
        };
        WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(3, value);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private void startVisibilityTask() {
        if (visibilityTaskId != -1) return;
        visibilityTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (noclipPlayers.isEmpty()) {
                Bukkit.getScheduler().cancelTask(visibilityTaskId);
                visibilityTaskId = -1;
                return;
            }
            for (UUID uuid : noclipPlayers.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (other.equals(p)) continue;
                    other.showPlayer(plugin, p);
                }
            }
        }, 10L, 10L);
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(noclipPlayers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) disableNoClip(p);
        }
        noclipPlayers.clear();
        if (visibilityTaskId != -1) {
            Bukkit.getScheduler().cancelTask(visibilityTaskId);
            visibilityTaskId = -1;
        }
    }

    public boolean isNoClip(UUID uuid) {
        return noclipPlayers.containsKey(uuid);
    }

    public GameMode getOriginalGameMode(UUID uuid) {
        return noclipPlayers.getOrDefault(uuid, GameMode.SURVIVAL);
    }

    public LifeMod getPlugin() {
        return plugin;
    }
}
