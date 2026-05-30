package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.IVanishService;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.entity.Player;

public class VanishAction implements IStaffAction {

    private final DebugManager debug;

    public VanishAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void execute(StaffActionContext context) {
        Player player = context.getPlayer();
        debug.log("vanish", "VanishAction.execute for " + player.getName());

        IVanishService vanishService = LifeMod.getInstance().getVanishService();
        ILangService lang = ServiceRegistry.get(ILangService.class);
        boolean currentState = vanishService.isVanished(player.getUniqueId());
        vanishService.setVanished(player, !currentState, false);

        if (currentState) {
            player.sendMessage(lang.getMessage("mod.items.vanish.visible"));
        } else {
            player.sendMessage(lang.getMessage("mod.items.vanish.vanished"));
        }
    }
}
