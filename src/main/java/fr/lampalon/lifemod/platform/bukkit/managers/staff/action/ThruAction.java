package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockIterator;

public class ThruAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        // Simple passthrough logic
        BlockIterator iterator = new BlockIterator(player, 10); // Max 10 blocks depth
        Block lastSolid = null;
        
        while (iterator.hasNext()) {
            Block b = iterator.next();
            if (b.getType().isSolid()) {
                lastSolid = b;
            } else if (lastSolid != null && !b.getType().isSolid()) {
                // Found air after solid
                Location loc = b.getLocation().add(0.5, 0, 0.5);
                loc.setYaw(player.getLocation().getYaw());
                loc.setPitch(player.getLocation().getPitch());
                player.teleport(loc);
                player.sendMessage(lang.getMessage("mod.items.navigation.thru"));
                return;
            }
        }
        player.sendMessage(lang.getMessage("mod.items.navigation.no-safe-spot"));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        // Ignored
    }
}
