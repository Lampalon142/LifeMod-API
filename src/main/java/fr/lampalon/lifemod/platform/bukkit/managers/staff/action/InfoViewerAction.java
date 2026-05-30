package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Deque;

public class InfoViewerAction implements IStaffAction {

    private final DebugManager debug;

    public InfoViewerAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        debug.log("staff", "InfoViewerAction.onInteract for " + player.getName());
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        player.sendMessage(lang.getMessage("mod.items.info.usage"));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        debug.log("staff", "InfoViewerAction.onInteractEntity for " + player.getName());
        if (!(event.getRightClicked() instanceof Player)) {
            debug.log("staff", "InfoViewerAction: target not a player, it's " + event.getRightClicked().getType());
            return;
        }
        Player target = (Player) event.getRightClicked();
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);

        int ping = target.getPing();
        Deque<Long> clicks = LifeMod.getInstance().getCpsMap().get(target.getUniqueId());
        int cps = (clicks != null) ? (int) clicks.stream().filter(t -> System.currentTimeMillis() - t <= 1000).count() : 0;
        debug.log("staff", "InfoViewerAction: ping=" + ping + " cps=" + cps + " gm=" + target.getGameMode());

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
