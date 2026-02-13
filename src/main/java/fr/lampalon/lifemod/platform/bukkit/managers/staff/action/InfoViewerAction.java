package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Deque;

public class InfoViewerAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.info.usage")));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player target = (Player) event.getRightClicked();

        int ping = target.getPing();
        Deque<Long> clicks = LifeMod.getInstance().getCpsMap().get(target.getUniqueId());
        int cps = (clicks != null) ? (int) clicks.stream().filter(t -> System.currentTimeMillis() - t <= 1000).count() : 0;

        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.info.header")));
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.info.title").replace("%target%", target.getName())));
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.info.ping").replace("%ping%", String.valueOf(ping))));
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.info.cps").replace("%cps%", String.valueOf(cps))));
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.info.gamemode").replace("%gamemode%", target.getGameMode().name())));
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.info.location")
                .replace("%world%", target.getWorld().getName())
                .replace("%x%", String.valueOf(target.getLocation().getBlockX()))
                .replace("%y%", String.valueOf(target.getLocation().getBlockY()))
                .replace("%z%", String.valueOf(target.getLocation().getBlockZ()))));
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.info.footer")));
    }
}
