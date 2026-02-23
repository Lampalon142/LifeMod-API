package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.BukkitDatabaseUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuit implements Listener {
    private final DebugManager debug = LifeMod.getInstance().getDebugManager();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location location = player.getLocation();
        LifeMod plugin = LifeMod.getInstance();
        boolean isMod = plugin.getStaffModeManager().isMod(player);

        // Save data
        DatabaseProvider db = plugin.getDatabaseManager().getDatabaseProvider();
        
        // Update PlayerData with current staff status
        fr.lampalon.lifemod.common.model.PlayerData data = db.getPlayerData(uuid);
        if (data != null) {
            data.setInStaffMode(isMod);
            data.setLastSeen(System.currentTimeMillis());
            db.savePlayerData(data);
        }

        db.saveCoords(uuid, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        
        // Remove from local memory so the server doesn't "remember" them as staff next time they join
        plugin.getStaffModeManager().cleanupMemoryOnQuit(uuid);

        // Remove from vanished list (Internal cleanup)
        plugin.getVanishService().getVanishedPlayers().remove(uuid);

        // Remove AntiCheat Data
        fr.lampalon.lifemod.common.anticheat.AntiCheatService acService = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.anticheat.AntiCheatService.class);
        if (acService != null) {
            acService.removePlayerData(uuid);
        }

        debug.log("playerquit", player.getName() + " data saved on quit.");
    }
}
