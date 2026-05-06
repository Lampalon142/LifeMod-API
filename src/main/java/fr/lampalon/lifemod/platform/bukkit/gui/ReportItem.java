package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReportItem extends AbstractItem {

    private final Report report;

    public ReportItem(Report report) {
        this.report = report;
    }

    @Override
    public ItemProvider getItemProvider() {
        OfflinePlayer target = Bukkit.getOfflinePlayer(report.getTargetUuid());
        OfflinePlayer reporter = Bukkit.getOfflinePlayer(report.getReporterUuid());
        
        String targetName = target != null && target.getName() != null ? target.getName() : report.getTargetUuid().toString();
        String reporterName = reporter != null && reporter.getName() != null ? reporter.getName() : report.getReporterUuid().toString();
        
        String targetStatus = (target != null && target.isOnline()) ? "§aOnline" : "§cOffline";
        String reporterStatus = (reporter != null && reporter.isOnline()) ? "§aOnline" : "§cOffline";

        List<String> loreTemplate = LifeMod.getInstance().getLangConfig().getStringList("reports.gui.item-lore");
        List<String> lore = loreTemplate.stream()
                .map(line -> MessageUtil.formatMessage(line
                        .replace("%target%", targetName)
                        .replace("%target_status%", targetStatus)
                        .replace("%reporter%", reporterName)
                        .replace("%reporter_status%", reporterStatus)
                        .replace("%reason%", report.getReason())
                        .replace("%status%", MessageUtil.getStatusDisplayName(report.getStatus()))
                        .replace("%server%", report.getServerName())
                        .replace("%date%", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(report.getCreatedAt())))
                        .replace("%uuid%", report.getUuid().toString())
                ))
                .collect(Collectors.toList());

        String displayName = LifeMod.getInstance().getLangConfig()
                .getString("reports.gui.item-name", "Report: %uuid%")
                .replace("%uuid%", report.getUuid().toString().substring(0, 8));

        ItemBuilder builder = new ItemBuilder(Material.PAPER).setDisplayName(MessageUtil.formatMessage(displayName));
        for (String line : lore) {
            builder.addLoreLines(line);
        }
        
        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
    }

    private String getNameFromUuid(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer != null && offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
    }
}
