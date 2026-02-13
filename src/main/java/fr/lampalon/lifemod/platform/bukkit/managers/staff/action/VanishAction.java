package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.IVanishService;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class VanishAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        toggleVanish(player);
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        toggleVanish(player);
    }

    private void toggleVanish(Player player) {
        IVanishService vanishService = LifeMod.getInstance().getVanishService();
        boolean currentState = vanishService.isVanished(player.getUniqueId());
        vanishService.setVanished(player, !currentState, false);
        
        if (currentState) {
            player.sendMessage(MessageUtil.formatMessage("&cYou are now VISIBLE."));
        } else {
            player.sendMessage(MessageUtil.formatMessage("&aYou are now VANISHED."));
        }
    }
}
