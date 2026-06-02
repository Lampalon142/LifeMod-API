package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.entity.Player;

public class FreezeAction implements IStaffAction {

    private final DebugManager debug;

    public FreezeAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void execute(StaffActionContext context) {
        Player player = context.getPlayer();
        ILangService lang = ServiceRegistry.get(ILangService.class);

        if (!context.hasEntityTarget() || !(context.getTargetEntity() instanceof Player)) {
            debug.log("freeze", "FreezeAction: no player target for " + player.getName());
            player.sendMessage(lang.getMessage("mod.items.no-target"));
            return;
        }

        Player target = (Player) context.getTargetEntity();
        debug.log("freeze", "FreezeAction: target=" + target.getName());

        LifeMod plugin = LifeMod.getInstance();
        if (plugin.getFreezeManager().isPlayerFrozen(target.getUniqueId())) {
            debug.log("freeze", "FreezeAction: unfreezing " + target.getName());
            plugin.getFreezeManager().unfreezePlayer(player, target);
            player.sendMessage(lang.getMessage("mod.items.freeze-off", "%player%", target.getName()));
        } else {
            debug.log("freeze", "FreezeAction: freezing " + target.getName());
            plugin.getFreezeManager().freezePlayer(player, target);
            player.sendMessage(lang.getMessage("mod.items.freeze-on", "%player%", target.getName()));
        }
    }
}
