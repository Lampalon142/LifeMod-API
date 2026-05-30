package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InspectorAction implements IStaffAction {

    private final DebugManager debug;

    public InspectorAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        debug.log("staff", "InspectorAction.onInteract for " + player.getName());
        Block block = event.getClickedBlock();
        if (block != null && block.getState() instanceof Container) {
            debug.log("staff", "InspectorAction: silent opening " + block.getType() + " for " + player.getName());
            event.setCancelled(true);
            
            Container container = (Container) block.getState();
            Inventory realInv = container.getInventory();
            
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            
            Inventory virtualInv = Bukkit.createInventory(null, realInv.getSize(), lang.getMessage("mod.items.inspector.silent-title", "%block%", block.getType().name()));
            virtualInv.setContents(realInv.getContents());
            
            player.openInventory(virtualInv);
            player.sendMessage(lang.getMessage("mod.items.inspector.silent-open", "%block%", block.getType().name()));
        } else {
            debug.log("staff", "InspectorAction: no container block clicked, block=" + (block == null ? "null" : block.getType().name()));
        }
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        debug.log("staff", "InspectorAction.onInteractEntity for " + player.getName());
        if (event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            debug.log("staff", "InspectorAction: inspecting player " + target.getName());
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            player.openInventory(target.getInventory());
            player.sendMessage(lang.getMessage("mod.items.inspector.inspect-player", "%target%", target.getName()));
        } else {
            debug.log("staff", "InspectorAction: right-clicked entity is not a Player, it's " + event.getRightClicked().getType());
        }
    }
}
