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
import org.bukkit.util.BlockIterator;

public class ThruAction implements IStaffAction {

    @Override
    public void execute(StaffActionContext context) {
        if (context.getInteractEvent() == null) return;

        Player player = context.getPlayer();
        ILangService lang = ServiceRegistry.get(ILangService.class);
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        int maxDepth = (int) config.getDouble("modules.mod-mode.items.navigation.thru-max-depth", 10.0);
        BlockIterator iterator = new BlockIterator(player, maxDepth);
        Block lastSolid = null;

        while (iterator.hasNext()) {
            Block b = iterator.next();
            if (b.getType().isSolid()) {
                lastSolid = b;
            } else if (lastSolid != null && !b.getType().isSolid()) {
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
}
