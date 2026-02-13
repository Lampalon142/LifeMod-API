package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomTpAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);
        
        if (online.isEmpty()) {
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.random-tp.no-players")));
            return;
        }
        
        Player target = online.get(new Random().nextInt(online.size()));
        player.teleport(target.getLocation());
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.random-tp.success").replace("%target%", target.getName())));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        // Not used
    }
}
