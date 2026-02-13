package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.FreezeManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class FreezeAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        // Freeze usually requires a target
        player.sendMessage(MessageUtil.formatMessage("&cYou must click on a player to freeze them."));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            FreezeManager fm = LifeMod.getInstance().getFreezeManager();
            
            if (fm.isPlayerFrozen(target.getUniqueId())) {
                fm.unfreezePlayer(player, target);
                player.sendMessage(MessageUtil.formatMessage("&aUnfrozen &e" + target.getName()));
            } else {
                fm.freezePlayer(player, target);
                player.sendMessage(MessageUtil.formatMessage("&cFrozen &e" + target.getName()));
            }
        }
    }
}
