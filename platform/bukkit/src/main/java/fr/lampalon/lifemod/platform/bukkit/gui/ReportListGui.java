package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReportListGui extends PagedAbstractGui {

    private final DatabaseProvider db;

    public ReportListGui(Player player) {
        super(player);
        this.db = LifeMod.getInstance().getDatabaseManager().getDatabaseProvider();
    }

    @Override
    protected List<Item> getListItems() {
        List<Report> reports = db.getAllReports(1000, 0);
        List<Item> items = new ArrayList<>();

        for (Report report : reports) {
            items.add(new ReportItem(report));
        }

        return items;
    }

    @Override
    protected String getTitleKey() {
        return "reports.gui.main-title";
    }

    @Override
    protected String getTitle() {
        List<Report> all = db.getAllReports(Integer.MAX_VALUE, 0);
        return lang.getMessage(getTitleKey(), "%page%", "1", "%total%", String.valueOf(all.size()));
    }

    @Override
    public Gui buildGui() {
        List<Report> all = db.getAllReports(Integer.MAX_VALUE, 0);
        int total = all.size();
        return PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# # # < R > # # #")
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', createBorder())
                .addIngredient('<', new PageItem(false) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        Material mat = Material.valueOf(
                                config.getString("gui.pagination.previous-material", "ARROW"));
                        ItemBuilder builder = new ItemBuilder(mat);
                        builder.setDisplayName(lang.getMessage("gui.pagination.previous"));
                        if (!gui.hasPreviousPage())
                            builder.addLoreLines(lang.getMessage("gui.pagination.first-page"));
                        return builder;
                    }
                })
                .addIngredient('>', new PageItem(true) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        Material mat = Material.valueOf(
                                config.getString("gui.pagination.next-material", "ARROW"));
                        ItemBuilder builder = new ItemBuilder(mat);
                        builder.setDisplayName(lang.getMessage("gui.pagination.next"));
                        if (!gui.hasNextPage())
                            builder.addLoreLines(lang.getMessage("gui.pagination.last-page"));
                        return builder;
                    }
                })
                .addIngredient('R', new AbstractItem() {
                    @Override
                    public ItemProvider getItemProvider() {
                        Material mat = Material.valueOf(
                                config.getString("gui.reports.refresh-material", "SUNFLOWER"));
                        return new ItemBuilder(mat)
                                .setDisplayName(lang.getMessage("reports.gui.refresh"));
                    }

                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
                                            @NotNull InventoryClickEvent event) {
                        new ReportListGui(player).open();
                    }
                })
                .setContent(getListItems())
                .build();
    }

    private class ReportItem extends AbstractItem {
        private final Report report;
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        ReportItem(Report report) {
            this.report = report;
        }

        @Override
        public ItemProvider getItemProvider() {
            Material mat = Material.valueOf(
                    config.getString("gui.reports.item-material", "PAPER"));
            ItemBuilder builder = new ItemBuilder(mat);

            builder.setDisplayName(lang.getMessage("reports.gui.item-name",
                    "%id%", String.valueOf(report.getId()),
                    "%target%", report.getTargetName()));

            for (String line : lang.getStringList("reports.gui.item-lore")) {
                line = line.replace("%target%", report.getTargetName() != null ? report.getTargetName() : "?");
                line = line.replace("%reporter%", report.getReporterName() != null ? report.getReporterName() : "?");
                line = line.replace("%reason%", report.getReason());
                line = line.replace("%status%", lang.getMessage("reports.status." + report.getStatus().name()));
                line = line.replace("%server%", report.getServerName() != null ? report.getServerName() : "?");
                line = line.replace("%date%", sdf.format(new Date(report.getCreatedAt())));
                builder.addLoreLines(lang.formatMessage(line));
            }

            return builder;
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
                                @NotNull InventoryClickEvent event) {
            new ReportDetailGui(player, report.getId()).open();
        }
    }
}
