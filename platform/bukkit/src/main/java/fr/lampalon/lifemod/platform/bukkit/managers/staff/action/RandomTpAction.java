package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomTpAction implements IStaffAction {

    private final DebugManager debug;

    public RandomTpAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void execute(StaffActionContext context) {
        Player player = context.getPlayer();
        ILangService lang = ServiceRegistry.get(ILangService.class);
        debug.log("staff", "RandomTpAction.execute for " + player.getName());

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);

        if (online.isEmpty()) {
            debug.log("staff", "RandomTpAction: no other players online");
            player.sendMessage(lang.getMessage("mod.items.random-tp.no-players"));
            return;
        }

        Player target = online.get(new Random().nextInt(online.size()));
        debug.log("staff", "RandomTpAction: teleporting " + player.getName() + " to " + target.getName());
        player.teleport(target.getLocation());
        player.sendMessage(lang.getMessage("mod.items.random-tp.success", "%target%", target.getName()));
    }
}
