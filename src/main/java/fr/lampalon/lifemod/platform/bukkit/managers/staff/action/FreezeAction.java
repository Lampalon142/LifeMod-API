package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class FreezeAction implements IStaffAction {

    private final DebugManager debug;

    public FreezeAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        debug.log("freeze", "FreezeAction.onInteract (no target) for " + player.getName());
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        player.sendMessage(lang.getMessage("mod.items.no-target"));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        debug.log("freeze", "FreezeAction.onInteractEntity for " + player.getName() + " target=" + event.getRightClicked().getName());
        if (event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            LifeMod plugin = LifeMod.getInstance();
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            
            if (plugin.getFreezeManager().isPlayerFrozen(target.getUniqueId())) {
                debug.log("freeze", "FreezeAction: unfreezing " + target.getName());
                plugin.getFreezeManager().unfreezePlayer(player, target);
                player.sendMessage(lang.getMessage("mod.items.freeze-off", "%player%", target.getName()));
            } else {
                debug.log("freeze", "FreezeAction: freezing " + target.getName());
                plugin.getFreezeManager().freezePlayer(player, target);
                player.sendMessage(lang.getMessage("mod.items.freeze-on", "%player%", target.getName()));
            }
        } else {
            debug.log("freeze", "FreezeAction: right-clicked entity is not a Player, it's " + event.getRightClicked().getType());
        }
    }
}
