package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.PlayerManager;
import fr.lampalon.lifemod.platform.bukkit.managers.VanishedManager;
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

    public PlayerQuit() {
        Bukkit.getOnlinePlayers().stream()
                .filter(PlayerManager::isInModerationMod)
                .forEach(p -> {
                    PlayerManager.getFromPlayer(p).destroy();
                    debug.log("mod", p.getName() + " moderation mode destroyed on plugin reload.");
                });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location location = player.getLocation();

        DatabaseProvider db = LifeMod.getInstance().getDatabaseManager().getDatabaseProvider();
        db.saveCoords(uuid, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        db.saveRawInventory(uuid, BukkitDatabaseUtil.serializeInventory(player.getInventory()));
        
        VanishedManager.handlePlayerQuit(player);

        debug.log("playerquit", player.getName() + " data saved on quit.");
    }
}


