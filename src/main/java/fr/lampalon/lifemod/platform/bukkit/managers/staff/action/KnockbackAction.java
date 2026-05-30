package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.entity.Player;

public class KnockbackAction implements IStaffAction {

    @Override
    public void execute(StaffActionContext context) {
        if (!context.hasEntityTarget() || !(context.getTargetEntity() instanceof Player)) return;

        Player player = context.getPlayer();
        Player target = (Player) context.getTargetEntity();
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        ILangService lang = ServiceRegistry.get(ILangService.class);

        double multiplier = config.getDouble("modules.mod-mode.items.kbtester.knockback-multiplier", 0.5);
        double y = config.getDouble("modules.mod-mode.items.kbtester.knockback-y", 0.4);
        target.setVelocity(player.getLocation().getDirection().multiply(multiplier).setY(y));
        player.sendMessage(lang.getMessage("mod.items.kb-tester.applied", "%target%", target.getName()));
    }
}
