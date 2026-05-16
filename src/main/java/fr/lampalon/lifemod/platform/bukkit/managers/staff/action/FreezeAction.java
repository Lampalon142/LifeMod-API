package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class FreezeAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        player.sendMessage(lang.getMessage("mod.items.no-target"));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            LifeMod plugin = LifeMod.getInstance();
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            
            if (plugin.getFreezeManager().isPlayerFrozen(target.getUniqueId())) {
                plugin.getFreezeManager().unfreezePlayer(player, target);
                player.sendMessage(lang.getMessage("mod.items.freeze-off", "%player%", target.getName()));
            } else {
                plugin.getFreezeManager().freezePlayer(player, target);
                player.sendMessage(lang.getMessage("mod.items.freeze-on", "%player%", target.getName()));
            }
        }
    }
}
