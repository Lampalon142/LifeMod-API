package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class StaffPhysicalListener implements Listener {

    private final StaffModeManager staffModeManager;

    public StaffPhysicalListener(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }

    @EventHandler
    public void onPhysicalInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        LifeMod plugin = LifeMod.getInstance();
        
        // If staff is in mod mode AND vanished
        if (staffModeManager.isMod(player) && plugin.getVanishService().isVanished(player.getUniqueId())) {
            // Cancel physical interactions (pressure plates, tripwires, sculk sensors)
            if (event.getAction() == Action.PHYSICAL) {
                event.setCancelled(true);
            }
        }
    }
}
