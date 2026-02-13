package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;

public class InfoViewerAction implements IStaffAction {

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        // Self info or instruction
        player.sendMessage(MessageUtil.formatMessage("&bRight-click a player to view their info."));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player target = (Player) event.getRightClicked();
        
        int ping = getPing(target);
        int cps = getCPS(target);
        
        player.sendMessage(MessageUtil.formatMessage("&8&m----------------------------------------"));
        player.sendMessage(MessageUtil.formatMessage("&6&lInformation Viewer &7- &e" + target.getName()));
        player.sendMessage(MessageUtil.formatMessage(" "));
        player.sendMessage(MessageUtil.formatMessage("&e• &fPing: &b" + ping + "ms"));
        player.sendMessage(MessageUtil.formatMessage("&e• &fCPS: &b" + cps));
        player.sendMessage(MessageUtil.formatMessage("&e• &fGamemode: &b" + target.getGameMode().name()));
        player.sendMessage(MessageUtil.formatMessage("&e• &fLocation: &b" + target.getWorld().getName() + " " + target.getLocation().getBlockX() + "," + target.getLocation().getBlockY() + "," + target.getLocation().getBlockZ()));
        player.sendMessage(MessageUtil.formatMessage(" "));
        player.sendMessage(MessageUtil.formatMessage("&8&m----------------------------------------"));
    }

    private int getPing(Player player) {
        try {
            return player.getPing();
        } catch (NoSuchMethodError e) {
            // Fallback for older Spigot versions if necessary
            return 0;
        }
    }

    private int getCPS(Player target) {
        Map<UUID, Deque<Long>> map = LifeMod.getInstance().getCpsMap();
        Deque<Long> clicks = map.get(target.getUniqueId());
        if (clicks == null) return 0;
        long now = System.currentTimeMillis();
        clicks.removeIf(click -> now - click > 1000);
        return clicks.size();
    }
}
