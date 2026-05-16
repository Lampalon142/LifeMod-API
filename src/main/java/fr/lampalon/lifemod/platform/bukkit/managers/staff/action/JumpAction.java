package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class JumpAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        Block target = player.getTargetBlock((Set<Material>) null, 100);
        if (target == null || target.getType() == Material.AIR) {
            player.sendMessage(lang.getMessage("mod.items.navigation.no-target"));
            return;
        }
        
        Location loc = target.getLocation().add(0.5, 1.0, 0.5);
        loc.setYaw(player.getLocation().getYaw());
        loc.setPitch(player.getLocation().getPitch());
        player.teleport(loc);
        player.sendMessage(lang.getMessage("mod.items.navigation.jump"));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        // Jump usually ignores entities
    }
}
