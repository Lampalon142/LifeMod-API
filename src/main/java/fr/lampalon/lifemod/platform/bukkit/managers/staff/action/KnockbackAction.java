package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

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
        // Simulating Knockback
        target.setVelocity(player.getLocation().getDirection().multiply(0.5).setY(0.4));
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.kb-tester.applied").replace("%target%", target.getName())));
    }
}

