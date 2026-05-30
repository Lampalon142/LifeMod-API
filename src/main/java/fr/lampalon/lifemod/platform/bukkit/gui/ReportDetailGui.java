package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Report;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportDetailGui extends AbstractGui {

    private final Report report;

    public ReportDetailGui(Player player, Report report) {
        super(player);
        this.report = report;
    }

    @Override
    public Gui buildGui() {
        OfflinePlayer reporter = Bukkit.getOfflinePlayer(report.getReporterUuid());
        OfflinePlayer target = Bukkit.getOfflinePlayer(report.getTargetUuid());

        String reporterName = reporter != null && reporter.getName() != null ? reporter.getName() : report.getReporterUuid().toString();
        String targetName = target != null && target.getName() != null ? target.getName() : report.getTargetUuid().toString();

        String dateFormat = config.getString("server.date-format", "dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

        Material reporterMat = Material.valueOf(config.getString("gui.reports.detail.reporter-material", "PLAYER_HEAD"));
        Material targetMat = Material.valueOf(config.getString("gui.reports.detail.target-material", "PLAYER_HEAD"));
        Material infoMat = Material.valueOf(config.getString("gui.reports.detail.info-material", "BOOK"));
        Material teleportMat = Material.valueOf(config.getString("gui.reports.detail.teleport-material", "COMPASS"));
        Material assignMat = Material.valueOf(config.getString("gui.reports.detail.assign-material", "LIME_DYE"));
        Material closeMat = Material.valueOf(config.getString("gui.reports.detail.close-material", "RED_DYE"));
        Material backMat = Material.valueOf(config.getString("gui.reports.detail.back-material", "ARROW"));

        return Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# . R . I . T . #",
                        "# . . . . . . . #",
                        "# . L . A . C . #",
                        "# . . . . . . . #",
                        "# # # # B # # # #")
                .addIngredient('#', createBorder())
                .addIngredient('R', new SimpleItem(new ItemBuilder(reporterMat)
                        .setDisplayName(lang.getMessage("reports.gui.detail.reporter", "%player%", reporterName))
                        .addLoreLines(lang.getMessage("reports.gui.detail.status-prefix") + (reporter != null && reporter.isOnline() ? lang.getMessage("reports.status.online", "&aOnline") : lang.getMessage("reports.status.offline", "&cOffline")))))
                .addIngredient('T', new SimpleItem(new ItemBuilder(targetMat)
                        .setDisplayName(lang.getMessage("reports.gui.detail.target", "%player%", targetName))
                        .addLoreLines(lang.getMessage("reports.gui.detail.status-prefix") + (target != null && target.isOnline() ? lang.getMessage("reports.status.online", "&aOnline") : lang.getMessage("reports.status.offline", "&cOffline")))))
                .addIngredient('I', new SimpleItem(new ItemBuilder(infoMat)
                        .setDisplayName(lang.getMessage("reports.gui.detail.info-title"))
                        .addLoreLines(
                                lang.getMessage("reports.gui.detail.info-reason", "%reason%", report.getReason()),
                                lang.getMessage("reports.gui.detail.info-server", "%server%", report.getServerName()),
                                lang.getMessage("reports.gui.detail.info-status", "%status%", report.getStatus().name()),
                                lang.getMessage("reports.gui.detail.info-date", "%date%", sdf.format(new Date(report.getCreatedAt())))
                        )))
                .addIngredient('L', new SimpleItem(new ItemBuilder(teleportMat)
                        .setDisplayName(lang.getMessage("reports.gui.detail.teleport"))
                        .addLoreLines(lang.getStringList("reports.gui.detail.teleport-lore").toArray(String[]::new))))
                .addIngredient('A', new SimpleItem(new ItemBuilder(assignMat)
                        .setDisplayName(lang.getMessage("reports.gui.detail.assign"))
                        .addLoreLines(lang.getStringList("reports.gui.detail.assign-lore").toArray(String[]::new))))
                .addIngredient('C', new SimpleItem(new ItemBuilder(closeMat)
                        .setDisplayName(lang.getMessage("reports.gui.detail.close"))
                        .addLoreLines(lang.getStringList("reports.gui.detail.close-lore").toArray(String[]::new))))
                .addIngredient('B', new SimpleItem(new ItemBuilder(backMat).setDisplayName(lang.getMessage("reports.gui.detail.back")), click -> {
                    player.closeInventory();
                }))
                .build();
    }

    @Override
    protected String getTitle() {
        return lang.getMessage("reports.gui.detail.title-prefix") + report.getUuid().toString().substring(0, 8);
    }

    @Override
    protected String getTitleKey() {
        return "report.detail.title";
    }
}
