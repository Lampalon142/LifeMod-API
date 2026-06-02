package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.entity.Player;

import java.util.Deque;

public class InfoViewerAction implements IStaffAction {

    private final DebugManager debug;

    public InfoViewerAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void execute(StaffActionContext context) {
        Player player = context.getPlayer();
        ILangService lang = ServiceRegistry.get(ILangService.class);

        if (!context.hasEntityTarget() || !(context.getTargetEntity() instanceof Player)) {
            debug.log("staff", "InfoViewerAction: no player target for " + player.getName());
            player.sendMessage(lang.getMessage("mod.items.info.usage"));
            return;
        }

        Player target = (Player) context.getTargetEntity();
        debug.log("staff", "InfoViewerAction: viewing info for " + target.getName());

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
