package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportEvidence;
import fr.lampalon.lifemod.common.model.ReportStatus;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReportDetailGui extends AbstractGui {

    private final int reportId;
    private final DatabaseProvider db;
    private Report report;

    private static final Map<UUID, Integer> PENDING_EVIDENCE = new ConcurrentHashMap<>();
    private static boolean evidenceListenerRegistered = false;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private static final class EvidenceChatListener implements Listener {
        @EventHandler
        public void onChat(AsyncPlayerChatEvent event) {
            Integer reportId = PENDING_EVIDENCE.remove(event.getPlayer().getUniqueId());
            if (reportId == null) return;
            event.setCancelled(true);
            ILangService lang = ServiceRegistry.get(ILangService.class);
            Plugin plugin = Bukkit.getPluginManager().getPlugin("LifeMod");
            if (plugin != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    DatabaseProvider db = LifeMod.getInstance().getDatabaseManager().getDatabaseProvider();
                    String msg = event.getMessage();
                    if (msg.equalsIgnoreCase("cancel")) {
                        event.getPlayer().sendMessage(lang.getMessage("reports.gui.evidence-cancelled"));
                        return;
                    }
                    ReportEvidence evidence = new ReportEvidence();
                    evidence.setReportId(reportId);
                    evidence.setType(msg.startsWith("http://") || msg.startsWith("https://") ? "image_url" : "text");
                    evidence.setData(msg);
                    evidence.setAuthorUuid(event.getPlayer().getUniqueId());
                    evidence.setAuthorName(event.getPlayer().getName());
                    db.addEvidence(evidence);
                    event.getPlayer().sendMessage(
                            lang.getMessage("reports.gui.evidence-added",
                                    "%id%", String.valueOf(reportId)));
                    new ReportDetailGui(event.getPlayer(), reportId).open();
                });
            }
        }
    }

    private static void ensureListenerRegistered() {
        if (evidenceListenerRegistered) return;
        evidenceListenerRegistered = true;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LifeMod");
        if (plugin != null) {
            Bukkit.getPluginManager().registerEvents(new EvidenceChatListener(), plugin);
        }
    }

    public ReportDetailGui(Player player, int reportId) {
        super(player);
        this.reportId = reportId;
        this.db = LifeMod.getInstance().getDatabaseManager().getDatabaseProvider();
        this.report = db.getReportById(reportId);
    }

    @Override
    public Gui buildGui() {
        List<ReportEvidence> evidenceList = db.getEvidence(reportId);
        String loc = report.getLocationWorld() != null
                ? String.format("%s %.0f %.0f %.0f", report.getLocationWorld(),
                report.getLocationX(), report.getLocationY(), report.getLocationZ())
                : "N/A";
        String assignedTo = report.getAssignedTo() != null
                ? Bukkit.getOfflinePlayer(report.getAssignedTo()).getName()
                : lang.getMessage("reports.gui.detail-unassigned");

        Material statusMat = statusMaterial(report.getStatus());

        Item infoReporter = createInfoItem(Material.PLAYER_HEAD,
                "reports.gui.detail-reporter", "%reporter%", report.getReporterName());
        Item infoTarget = createInfoItem(Material.SKELETON_SKULL,
                "reports.gui.detail-target", "%target%", report.getTargetName());
        Item infoReason = createInfoItem(Material.BOOK,
                "reports.gui.detail-reason", "%reason%", report.getReason());
        Item infoServer = createInfoItem(Material.GRASS_BLOCK,
                "reports.gui.detail-server", "%server%", report.getServerName());
        Item infoStatus = createInfoItem(statusMat,
                "reports.gui.detail-status", "%status%", report.getStatus().name());
        Item infoDate = createInfoItem(Material.CLOCK,
                "reports.gui.detail-date", "%date%", DATE_FMT.format(new Date(report.getCreatedAt())));
        Item infoLocation = createInfoItem(Material.COMPASS,
                "reports.gui.detail-location", "%location%", loc);
        Item infoAssigned = createInfoItem(Material.NAME_TAG,
                "reports.gui.detail-assigned", "%assigned%", assignedTo);

        List<Item> evidenceItems = new ArrayList<>();
        for (ReportEvidence ev : evidenceList) {
            evidenceItems.add(createEvidenceItem(ev));
        }

        Item actionBack = new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                return new ItemBuilder(Material.ARROW)
                        .setDisplayName(lang.getMessage("reports.gui.back"));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
                                    @NotNull InventoryClickEvent event) {
                new ReportListGui(player).open();
            }
        };

        Item actionAssign = createStatusActionItem(Material.LIME_DYE,
                "reports.gui.assign", ReportStatus.ASSIGNED);
        Item actionClose = createStatusActionItem(Material.GREEN_DYE,
                "reports.gui.close", ReportStatus.CLOSED);
        Item actionReject = createStatusActionItem(Material.RED_DYE,
                "reports.gui.reject", ReportStatus.REJECTED);
        Item actionEvidence = new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                Material mat = Material.valueOf(
                        config.getString("gui.reports.evidence-material", "WRITABLE_BOOK"));
                return new ItemBuilder(mat)
                        .setDisplayName(lang.getMessage("reports.gui.add-evidence"));
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player,
                                    @NotNull InventoryClickEvent event) {
                player.closeInventory();
                player.sendMessage(lang.getMessage("reports.gui.evidence-prompt"));
                ensureListenerRegistered();
                PENDING_EVIDENCE.put(player.getUniqueId(), reportId);
            }
        };

        List<Item> infoSlots = new ArrayList<>();
        infoSlots.add(infoReporter);
        infoSlots.add(infoTarget);
        infoSlots.add(infoReason);
        infoSlots.add(infoServer);
        infoSlots.add(infoStatus);
        infoSlots.add(infoDate);
        infoSlots.add(infoLocation);
        infoSlots.add(infoAssigned);

        while (infoSlots.size() < 14) {
            infoSlots.add(new SimpleItem(new ItemBuilder(Material.AIR)));
        }

        List<Item> contentItems = new ArrayList<>();
        contentItems.addAll(infoSlots);
        contentItems.addAll(evidenceItems);

        while (contentItems.size() < 28) {
            contentItems.add(new SimpleItem(new ItemBuilder(Material.AIR)));
        }

        return PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# i i i i i i i #",
                        "# i i i i i i i #",
                        "# e e e e e e e #",
                        "# e e e e e e e #",
                        "# # B # A C R E #")
                .addIngredient('i', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('e', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', createBorder())
                .addIngredient('B', actionBack)
                .addIngredient('A', actionAssign)
                .addIngredient('C', actionClose)
                .addIngredient('R', actionReject)
                .addIngredient('E', actionEvidence)
                .setContent(contentItems)
                .build();
    }

    @Override
    protected String getTitleKey() {
        return "reports.gui.detail-title";
    }

    @Override
    protected String getTitle() {
        return lang.getMessage(getTitleKey(), "%id%", String.valueOf(reportId));
    }

    private Item createInfoItem(Material material, String nameKey, String... placeholders) {
        return new SimpleItem(new ItemBuilder(material)
                .setDisplayName(lang.getMessage(nameKey, placeholders)));
    }

    private Item createStatusActionItem(Material material, String nameKey, ReportStatus newStatus) {
        return new SimpleItem(new ItemBuilder(material)
                .setDisplayName(lang.getMessage(nameKey)),
                click -> {
                    UUID assignee = newStatus == ReportStatus.ASSIGNED
                            ? player.getUniqueId() : report.getAssignedTo();
                    db.updateReportStatus(reportId, newStatus.name().toLowerCase(), assignee);
                    report = db.getReportById(reportId);
                    player.sendMessage(lang.getMessage("reports.gui.status-updated",
                            "%status%", newStatus.name()));
                    new ReportListGui(player).open();
                });
    }

    private Item createEvidenceItem(ReportEvidence evidence) {
        String date = DATE_FMT.format(new Date(evidence.getCreatedAt()));
        String author = evidence.getAuthorName() != null ? evidence.getAuthorName() : "?";
        ItemBuilder builder = new ItemBuilder(Material.MAP);
        builder.setDisplayName(lang.getMessage("reports.gui.evidence-item-name",
                "%type%", evidence.getType(),
                "%author%", author));
        builder.addLoreLines(lang.getMessage("reports.gui.evidence-item-data",
                "%data%", evidence.getData()));
        builder.addLoreLines(lang.getMessage("reports.gui.evidence-item-date",
                "%date%", date));
        return new SimpleItem(builder);
    }

    private static Material statusMaterial(ReportStatus status) {
        switch (status) {
            case OPEN: return Material.LIME_CONCRETE;
            case ASSIGNED: return Material.YELLOW_CONCRETE;
            case CLOSED: return Material.GRAY_CONCRETE;
            case REJECTED: return Material.RED_CONCRETE;
            default: return Material.BARRIER;
        }
    }
}
