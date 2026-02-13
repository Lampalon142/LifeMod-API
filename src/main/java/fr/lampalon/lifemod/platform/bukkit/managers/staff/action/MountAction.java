package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MountAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        Entity target = event.getRightClicked();
        target.addPassenger(player);
        player.sendMessage(MessageUtil.formatMessage("&aMounted on &e" + target.getName()));
    }
}
