package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class InspectorAction implements IStaffAction {

    private final DebugManager debug;

    public InspectorAction() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    @Override
    public void execute(StaffActionContext context) {
        Player player = context.getPlayer();
        ILangService lang = ServiceRegistry.get(ILangService.class);

        if (context.hasEntityTarget() && context.getTargetEntity() instanceof Player) {
            Player target = (Player) context.getTargetEntity();
            debug.log("staff", "InspectorAction: inspecting player " + target.getName());
            player.openInventory(target.getInventory());
            player.sendMessage(lang.getMessage("mod.items.inspector.inspect-player", "%target%", target.getName()));
            return;
        }

        Block block = context.getClickedBlock();
        if (block != null && block.getState() instanceof Container) {
            debug.log("staff", "InspectorAction: silent opening " + block.getType() + " for " + player.getName());
            Container container = (Container) block.getState();
            Inventory realInv = container.getInventory();
            Inventory virtualInv = Bukkit.createInventory(null, realInv.getSize(),
                    lang.getMessage("mod.items.inspector.silent-title", "%block%", block.getType().name()));
            virtualInv.setContents(realInv.getContents());
            player.openInventory(virtualInv);
            player.sendMessage(lang.getMessage("mod.items.inspector.silent-open", "%block%", block.getType().name()));
            return;
        }

        debug.log("staff", "InspectorAction: no target for " + player.getName());
        player.sendMessage(lang.getMessage("mod.items.no-target"));
    }
}
