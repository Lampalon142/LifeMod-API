package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class FreezeAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.no-target")));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            LifeMod plugin = LifeMod.getInstance();
            
            if (plugin.getFreezeManager().isPlayerFrozen(target.getUniqueId())) {
                plugin.getFreezeManager().unfreezePlayer(player, target);
                player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("mod.items.freeze-off").replace("%player%", target.getName())));
            } else {
                plugin.getFreezeManager().freezePlayer(player, target);
                player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("mod.items.freeze-on").replace("%player%", target.getName())));
            }
        }
    }
}
