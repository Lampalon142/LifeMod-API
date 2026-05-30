package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class KnockbackAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        // Fallback if configured as Right Click
        if (event.getRightClicked() instanceof Player) {
            applyKnockback(player, (Player) event.getRightClicked());
        }
    }

    @Override
    public void onAttack(Player player, org.bukkit.entity.Entity target) {
        if (target instanceof Player) {
            applyKnockback(player, (Player) target);
        }
    }

    private void applyKnockback(Player player, Player target) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        double multiplier = config.getDouble("modules.mod-mode.items.kbtester.knockback-multiplier", 0.5);
        double y = config.getDouble("modules.mod-mode.items.kbtester.knockback-y", 0.4);
        target.setVelocity(player.getLocation().getDirection().multiply(multiplier).setY(y));
        player.sendMessage(lang.getMessage("mod.items.kb-tester.applied", "%target%", target.getName()));
    }
}

