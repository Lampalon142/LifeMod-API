package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.VanishService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerQuit implements Listener {
    private final LifeMod plugin;
    private final DebugManager debug;

    public PlayerQuit(LifeMod plugin) {
        this.plugin = plugin;
        this.debug = plugin.getDebugManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location location = player.getLocation();

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("player_count", Bukkit.getOnlinePlayers().size() - 1);
            props.put("session_seconds", 0);
            ph.capture("lifemod_player_quit", props);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseProvider db = plugin.getDatabaseManager().getDatabaseProvider();

            PlayerData data = db.getPlayerData(uuid);
            if (data != null) {
                debug.log("mod", "[PlayerQuit] " + player.getName() + " isInStaffMode preserved as " + data.isInStaffMode() + " on quit");
                data.setLastSeen(System.currentTimeMillis());
                db.savePlayerData(data);
            }

            db.saveCoords(uuid, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        });

        plugin.getStaffModeManager().cleanupMemoryOnQuit(uuid);
        plugin.getVanishService().getVanishedPlayers().remove(uuid);
        plugin.getFreezeManager().handleQuit(uuid);
        plugin.getSpectateManager().handleQuit(uuid);
        plugin.getNoteInputManager().handleQuit(uuid);
        if (plugin.getVanishService() instanceof VanishService vs) {
            vs.handleQuit(uuid);
        }
        plugin.getCpsMap().remove(uuid);
        plugin.getReplayPlayerManager().handleQuit(uuid);

        debug.log("playerquit", player.getName() + " data saved on quit.");
    }
}
