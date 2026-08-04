package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportEvidence;
import fr.lampalon.lifemod.common.model.ReportStatus;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.ChatContextManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatReportListener implements Listener {

    private final LifeMod plugin;
    private final ChatContextManager contextManager;
    private final boolean enabled;
    private final long confirmTimeoutMs;

    private static final Map<UUID, PendingReport> PENDING = new ConcurrentHashMap<>();
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");

    public ChatReportListener(LifeMod plugin) {
        this.plugin = plugin;
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        this.enabled = config.getBoolean("modules.chat-report.enabled", true);
        int maxMessages = config.getInt("modules.chat-report.max-messages", 30);
        this.confirmTimeoutMs = config.getLong("modules.chat-report.confirm-timeout", 60L) * 1000L;
        this.contextManager = new ChatContextManager(maxMessages);
    }

    public ChatContextManager getContextManager() {
        return contextManager;
    }

    public static void beginConfirmation(Player reporter, Player target) {
        UUID targetUuid = target.getUniqueId();
        String world = reporter.getWorld().getName();
        double x = reporter.getLocation().getX();
        double y = reporter.getLocation().getY();
        double z = reporter.getLocation().getZ();
        PENDING.put(reporter.getUniqueId(), new PendingReport(
                targetUuid, target.getName(), reporter.getName(),
                world, x, y, z, System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PendingReport pending = PENDING.remove(uuid);
        if (pending != null) {
            event.setCancelled(true);
            handleConfirmation(event, player, pending);
            return;
        }

        if (!enabled || event.isCancelled()) return;

        event.setCancelled(true);
        contextManager.record(uuid, player.getName(), event.getMessage(), System.currentTimeMillis());

        boolean showIcon = ServiceRegistry.get(IConfigurationService.class)
                .getBoolean("modules.chat-report.show-icon", true);
        broadcastChat(event, player, showIcon);
    }

    private void handleConfirmation(AsyncPlayerChatEvent event, Player reporter, PendingReport pending) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        if (System.currentTimeMillis() - pending.createdAt > confirmTimeoutMs) {
            reporter.sendMessage(lang.getMessage("chatreport.timeout"));
            return;
        }
        if (!event.getMessage().equalsIgnoreCase(lang.getMessage("chatreport.confirm"))) {
            reporter.sendMessage(lang.getMessage("chatreport.cancelled"));
            return;
        }
        createReport(reporter, pending);
    }

    private void createReport(Player reporter, PendingReport pending) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        String reasonKey = lang.getMessage("chatreport.reason");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseProvider db = plugin.getDatabaseManager().getDatabaseProvider();

            Report report = new Report();
            report.setReporterUuid(reporter.getUniqueId());
            report.setReporterName(reporter.getName());
            report.setTargetUuid(pending.targetUuid);
            report.setTargetName(pending.targetName);
            report.setReason(reasonKey);
            report.setServerName(ServiceRegistry.get(IConfigurationService.class)
                    .getString("server.name", "unknown"));
            report.setLocation(pending.world, pending.x, pending.y, pending.z);
            report.setStatus(ReportStatus.OPEN);
            int id = db.saveReport(report);

            java.util.List<ChatContextManager.StoredMessage> recent = contextManager.getRecent(pending.targetUuid);
            if (!recent.isEmpty()) {
                StringBuilder log = new StringBuilder();
                for (ChatContextManager.StoredMessage m : recent) {
                    log.append(TIME_FMT.format(new Date(m.getTimestamp())))
                            .append(' ').append(m.getName())
                            .append(": ").append(m.getMessage()).append('\n');
                }
                ReportEvidence evidence = new ReportEvidence();
                evidence.setReportId(id);
                evidence.setType("text");
                evidence.setData(log.toString().trim());
                evidence.setAuthorUuid(reporter.getUniqueId());
                evidence.setAuthorName(reporter.getName());
                db.addEvidence(evidence);
            }

            IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);

            Bukkit.getScheduler().runTask(plugin, () -> {
                reporter.sendMessage(lang.getMessage("chatreport.submitted", "%target%", pending.targetName));
                notifyStaff(lang, reporter.getName(), pending.targetName, config);
            });

            IMessagingService msg = ServiceRegistry.get(IMessagingService.class);
            if (msg != null) {
                String serverName = config.getString("server.name", "unknown");
                String json = "{\"action\":\"CREATE\",\"id\":" + id
                        + ",\"reporter\":\"" + reporter.getName()
                        + "\",\"target\":\"" + pending.targetName
                        + "\",\"reason\":\"" + reasonKey
                        + "\",\"server\":\"" + serverName + "\"}";
                msg.publish("lifemod:reports", json);
            }
        });
    }

    private void notifyStaff(ILangService lang, String reporterName, String targetName,
                             IConfigurationService config) {
        String serverName = config.getString("server.name", "unknown");
        String notifyMsg = lang.getMessage("reports.staff-notify",
                "%player%", reporterName,
                "%target%", targetName,
                "%reason%", lang.getMessage("chatreport.reason"),
                "%server%", serverName);

        TextComponent clickable = new TextComponent(notifyMsg);
        clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports"));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("lifemod.report.notify")) {
                online.spigot().sendMessage(clickable);
            }
        }
    }

    private void broadcastChat(AsyncPlayerChatEvent event, Player sender, boolean showIcon) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        TextComponent line = new TextComponent("");

        if (showIcon) {
            TextComponent icon = new TextComponent(lang.getMessage("chatreport.icon"));
            icon.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(lang.getMessage("chatreport.hover", "%player%", sender.getName())).create()));
            icon.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reportchat " + sender.getName()));
            line.addExtra(icon);
        }

        line.addExtra(new TextComponent(lang.getMessage("chatreport.name", "%name%", sender.getName())));
        line.addExtra(new TextComponent(": "));
        line.addExtra(new TextComponent(event.getMessage()));

        for (Player recipient : event.getRecipients()) {
            recipient.spigot().sendMessage(line);
        }

        plugin.getLogger().info(sender.getName() + ": " + event.getMessage());
    }

    private static final class PendingReport {
        private final UUID targetUuid;
        private final String targetName;
        private final String reporterName;
        private final String world;
        private final double x, y, z;
        private final long createdAt;

        private PendingReport(UUID targetUuid, String targetName, String reporterName,
                              String world, double x, double y, double z, long createdAt) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.reporterName = reporterName;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.createdAt = createdAt;
        }
    }
}