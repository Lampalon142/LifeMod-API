package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.IVanishService;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class VanishAction implements IStaffAction {

    private final DebugManager debug;

    public VanishAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        debug.log("vanish", "VanishAction.onInteract for " + player.getName());
        toggleVanish(player);
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        debug.log("vanish", "VanishAction.onInteractEntity for " + player.getName());
        toggleVanish(player);
    }

    private void toggleVanish(Player player) {
        IVanishService vanishService = LifeMod.getInstance().getVanishService();
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        boolean currentState = vanishService.isVanished(player.getUniqueId());
        debug.log("vanish", "toggleVanish: " + player.getName() + " currentState=" + currentState + " -> " + !currentState);
        vanishService.setVanished(player, !currentState, false);
        
        if (currentState) {
            player.sendMessage(lang.getMessage("mod.items.vanish.visible"));
        } else {
            player.sendMessage(lang.getMessage("mod.items.vanish.vanished"));
        }
    }
}
