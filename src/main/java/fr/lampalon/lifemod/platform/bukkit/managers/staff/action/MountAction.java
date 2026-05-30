package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MountAction implements IStaffAction {

    private final DebugManager debug;

    public MountAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        debug.log("staff", "MountAction.onInteract for " + player.getName() + " (no-op)");
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        Entity target = event.getRightClicked();
        debug.log("staff", "MountAction.onInteractEntity: " + player.getName() + " mounting " + target.getType() + (target instanceof Player ? " (" + ((Player) target).getName() + ")" : ""));
        target.addPassenger(player);
        player.sendMessage(lang.getMessage("mod.items.mount.success", "%target%", target.getName()));
    }
}
