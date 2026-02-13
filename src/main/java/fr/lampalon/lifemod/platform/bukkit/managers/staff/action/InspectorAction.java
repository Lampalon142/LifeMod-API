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
            
            // Create a virtual inventory to be truly silent
            Inventory virtualInv = Bukkit.createInventory(null, realInv.getSize(), MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.inspector.silent-title").replace("%block%", block.getType().name())));
            virtualInv.setContents(realInv.getContents());
            
            player.openInventory(virtualInv);
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.inspector.silent-open").replace("%block%", block.getType().name())));
        }
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            // Open real inventory for real-time interaction
            player.openInventory(target.getInventory());
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.inspector.inspect-player").replace("%target%", target.getName())));
        }
    }
}
