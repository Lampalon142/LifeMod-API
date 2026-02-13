package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerTeleportEvent implements Listener {
    private final DebugManager debug = LifeMod.getInstance().getDebugManager();

    @EventHandler
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        LifeMod plugin = LifeMod.getInstance();
        
        if (plugin.getVanishService().isVanished(player.getUniqueId())) {
            // Re-apply visibility for safety on some server versions
            plugin.getVanishService().updateAllForPlayer(player);
            debug.log("vanish", player.getName() + " teleported while vanished.");
        }
    }
}
