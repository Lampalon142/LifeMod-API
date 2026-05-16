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
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        player.sendMessage(lang.getMessage("mod.items.info.usage"));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player target = (Player) event.getRightClicked();
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);

        int ping = target.getPing();
        Deque<Long> clicks = LifeMod.getInstance().getCpsMap().get(target.getUniqueId());
        int cps = (clicks != null) ? (int) clicks.stream().filter(t -> System.currentTimeMillis() - t <= 1000).count() : 0;

        String gmKey = "mod.items.info.gamemodes." + target.getGameMode().name().toLowerCase();
        String gmName = lang.getMessage(gmKey);

        player.sendMessage(lang.getMessage("mod.items.info.header"));
        player.sendMessage(lang.getMessage("mod.items.info.title", "%target%", target.getName()));
        player.sendMessage(lang.getMessage("mod.items.info.ping", "%ping%", String.valueOf(ping)));
        player.sendMessage(lang.getMessage("mod.items.info.cps", "%cps%", String.valueOf(cps)));
        player.sendMessage(lang.getMessage("mod.items.info.gamemode", "%gamemode%", gmName));
        player.sendMessage(lang.getMessage("mod.items.info.location",
                "%world%", target.getWorld().getName(),
                "%x%", String.valueOf(target.getLocation().getBlockX()),
                "%y%", String.valueOf(target.getLocation().getBlockY()),
                "%z%", String.valueOf(target.getLocation().getBlockZ())));
        player.sendMessage(lang.getMessage("mod.items.info.footer"));
    }
}
