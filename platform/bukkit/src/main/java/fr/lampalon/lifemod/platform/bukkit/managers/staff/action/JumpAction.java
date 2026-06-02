package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class JumpAction implements IStaffAction {

    @Override
    public void execute(StaffActionContext context) {
        if (context.getInteractEvent() == null) return;

        Player player = context.getPlayer();
        ILangService lang = ServiceRegistry.get(ILangService.class);
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        int maxDist = (int) config.getDouble("modules.mod-mode.items.navigation.jump-max-distance", 100.0);
        Block target = player.getTargetBlock((Set<Material>) null, maxDist);
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
}
