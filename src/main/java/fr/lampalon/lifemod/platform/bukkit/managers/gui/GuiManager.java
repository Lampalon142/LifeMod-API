package fr.lampalon.lifemod.platform.bukkit.managers.gui;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.platform.bukkit.gui.ReportMainMenu;
import fr.lampalon.lifemod.platform.bukkit.gui.StaffNotesGui;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiManager {

    private final LifeMod plugin;
    private final Map<UUID, Integer> currentPages = new HashMap<>();

    public GuiManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player, List<Report> reports, int page, int itemsPerPage) {
        new ReportMainMenu(player, reports).open();
    }

    public void setCurrentPage(Player player, int page) {
        currentPages.put(player.getUniqueId(), page);
    }

    public int getCurrentPage(Player player) {
        return currentPages.getOrDefault(player.getUniqueId(), 0);
    }

    public void openStaffNotesMenu(Player player, Report report) {
        new StaffNotesGui(player, report).open();
    }
}

