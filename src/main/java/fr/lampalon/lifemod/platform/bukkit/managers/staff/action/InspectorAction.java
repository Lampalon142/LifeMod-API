package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
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

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && block.getState() instanceof Container) {
            event.setCancelled(true); // Cancel real opening (sound + animation)
            
            Container container = (Container) block.getState();
            Inventory realInv = container.getInventory();
            
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            
            // Create a virtual inventory to be truly silent
            Inventory virtualInv = Bukkit.createInventory(null, realInv.getSize(), lang.getMessage("mod.items.inspector.silent-title", "%block%", block.getType().name()));
            virtualInv.setContents(realInv.getContents());
            
            player.openInventory(virtualInv);
            player.sendMessage(lang.getMessage("mod.items.inspector.silent-open", "%block%", block.getType().name()));
        }
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            // Open real inventory for real-time interaction
            player.openInventory(target.getInventory());
            player.sendMessage(lang.getMessage("mod.items.inspector.inspect-player", "%target%", target.getName()));
        }
    }
}
