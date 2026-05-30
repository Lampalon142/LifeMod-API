package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class MountAction implements IStaffAction {

    private final DebugManager debug;

    public MountAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void execute(StaffActionContext context) {
        if (!context.hasEntityTarget()) return;

        Player player = context.getPlayer();
        Entity target = context.getTargetEntity();
        ILangService lang = ServiceRegistry.get(ILangService.class);

        debug.log("staff", "MountAction: " + player.getName() + " mounting " + target.getType()
                + (target instanceof Player ? " (" + ((Player) target).getName() + ")" : ""));

        target.addPassenger(player);
        player.sendMessage(lang.getMessage("mod.items.mount.success", "%target%", target.getName()));
    }
}
