package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
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

        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        fr.lampalon.lifemod.common.service.IConfigurationService config = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.IConfigurationService.class);
        String dateFormat = config.getString("server.date-format", "dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

        return Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# . R . I . T . #",
                        "# . . . . . . . #",
                        "# . L . A . C . #",
                        "# . . . . . . . #",
                        "# # # # B # # # #")
                .addIngredient('#', createBorder())
                .addIngredient('R', new SimpleItem(new ItemBuilder(Material.PLAYER_HEAD)
                        .setDisplayName(MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.reporter", "%player%", reporterName)))
                        .addLoreLines(MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.status-prefix") + (reporter != null && reporter.isOnline() ? lang.getMessage("reports.status.online", "&aOnline") : lang.getMessage("reports.status.offline", "&cOffline"))))))
                .addIngredient('T', new SimpleItem(new ItemBuilder(Material.PLAYER_HEAD)
                        .setDisplayName(MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.target", "%player%", targetName)))
                        .addLoreLines(MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.status-prefix") + (target != null && target.isOnline() ? lang.getMessage("reports.status.online", "&aOnline") : lang.getMessage("reports.status.offline", "&cOffline"))))))
                .addIngredient('I', new SimpleItem(new ItemBuilder(Material.BOOK)
                        .setDisplayName(MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.info-title")))
                        .addLoreLines(
                                MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.info-reason", "%reason%", report.getReason())),
                                MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.info-server", "%server%", report.getServerName())),
                                MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.info-status", "%status%", report.getStatus().name())),
                                MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.info-date", "%date%", sdf.format(new Date(report.getCreatedAt()))))
                        )))
                .addIngredient('L', new SimpleItem(new ItemBuilder(Material.COMPASS)
                        .setDisplayName(MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.teleport")))
                        .addLoreLines(lang.getStringList("reports.gui.detail.teleport-lore").stream().map(MessageUtil::formatMessage).toArray(String[]::new))))
                .addIngredient('A', new SimpleItem(new ItemBuilder(Material.LIME_DYE)
                        .setDisplayName(MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.assign")))
                        .addLoreLines(lang.getStringList("reports.gui.detail.assign-lore").stream().map(MessageUtil::formatMessage).toArray(String[]::new))))
                .addIngredient('C', new SimpleItem(new ItemBuilder(Material.RED_DYE)
                        .setDisplayName(MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.close")))
                        .addLoreLines(lang.getStringList("reports.gui.detail.close-lore").stream().map(MessageUtil::formatMessage).toArray(String[]::new))))
                .addIngredient('B', new SimpleItem(new ItemBuilder(Material.ARROW).setDisplayName(MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.back"))), click -> {
                    player.closeInventory();
                }))
                .build();
    }

    @Override
    protected String getTitle() {
        return MessageUtil.formatMessage(lang.getMessage("reports.gui.detail.title-prefix") + report.getUuid().toString().substring(0, 8));
    }

    @Override
    protected String getTitleKey() {
        return "report.detail.title";
    }
}
