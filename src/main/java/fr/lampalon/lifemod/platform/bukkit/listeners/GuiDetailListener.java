package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.database.DatabaseProvider;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportStatus;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.gui.ReportMainMenu;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class GuiDetailListener implements Listener {

    private final LifeMod plugin;

    public GuiDetailListener(LifeMod plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        String title = event.getView().getTitle();
        if (!title.contains("Report Details")) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        NamespacedKey reportKey = new NamespacedKey(plugin, "report_uuid");
        String uuidStr = item.getItemMeta().getPersistentDataContainer().get(reportKey, PersistentDataType.STRING);
        if (uuidStr == null) return;

        UUID reportUuid = UUID.fromString(uuidStr);
        Report report = plugin.getDatabaseManager().getDatabaseProvider().getReportByUuid(reportUuid);
        if (report == null) return;

        NamespacedKey actionKey = new NamespacedKey(plugin, "action_type");
        String action = item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        if ("teleport".equals(action)) {
            if (report.getLocationWorld() != null) {
                Location loc = new Location(Bukkit.getWorld(report.getLocationWorld()), report.getX(), report.getY(), report.getZ());
                player.teleport(loc);
                fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                player.sendMessage(lang.getMessage("reports.teleport-success"));
            }
        } else if ("back".equals(action)) {
            new ReportMainMenu(player, plugin.getDatabaseManager().getDatabaseProvider().getAllReports()).open();
        }
        // Autres actions à migrer progressivement...
    }
}

