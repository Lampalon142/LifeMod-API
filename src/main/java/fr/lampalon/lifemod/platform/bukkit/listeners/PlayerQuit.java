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

        // Disable Staff Mode if active
        if (plugin.getStaffModeManager().isMod(player)) {
            plugin.getStaffModeManager().disableStaffMode(player);
        }

        // Save data
        DatabaseProvider db = plugin.getDatabaseManager().getDatabaseProvider();
        db.saveCoords(uuid, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        db.saveRawInventory(uuid, BukkitDatabaseUtil.serializeInventory(player.getInventory()));
        
        // Remove from vanished list (Internal cleanup)
        plugin.getVanishService().getVanishedPlayers().remove(uuid);

        debug.log("playerquit", player.getName() + " data saved on quit.");
    }
}
