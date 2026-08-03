package fr.lampalon.lifemod.platform.bukkit.commands.impl.report;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportStatus;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ReportCommand extends LifeCommand {

    public ReportCommand() {
        super("report", "lifemod.report", true);
        setDescription("Report a player for rule breaking");
        setUsage("/report <player> <reason>");
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return List.of("<reason>");
        }
        return List.of();
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = context.getArgs();

        if (args.length < 2) {
            context.getSender().sendMessage(context.getLang().getMessage("reports.usage"));
            return;
        }

        Player reporter = context.getPlayer();
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            context.getSender().sendMessage(context.getLang().getMessage("reports.not-found"));
            return;
        }

        if (target.equals(reporter)) {
            context.getSender().sendMessage(context.getLang().getMessage("reports.yourself"));
            return;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        String serverName = config.getString("server.name", "unknown");

        Report report = new Report();
        report.setReporterUuid(reporter.getUniqueId());
        report.setReporterName(reporter.getName());
        report.setTargetUuid(target.getUniqueId());
        report.setTargetName(target.getName());
        report.setReason(reason);
        report.setServerName(serverName);
        report.setLocation(reporter.getLocation().getWorld().getName(),
                reporter.getLocation().getX(),
                reporter.getLocation().getY(),
                reporter.getLocation().getZ());
        report.setStatus(ReportStatus.OPEN);

        LifeMod plugin = context.getPlugin();
        DatabaseProvider db = plugin.getDatabaseManager().getDatabaseProvider();
        int reportId = db.saveReport(report);

        // Capture replay for the reported player
        ReplayManager replayManager = plugin.getReplayManager();
        ReplaySession replaySession = replayManager.getSession(target.getUniqueId());
        if (replaySession != null && replaySession.isRecording()) {
            String replayName = replaySession.getSessionName();
            replayManager.stopRecording(target.getUniqueId());
            db.markReplayAsReport(replayName);
            db.setReportReplayId(reportId, replayName);

            reporter.sendMessage(context.getLang().getMessage("reports.replay-captured",
                    "%player%", target.getName()));

            // Start a new recording session so the player continues being recorded
            org.bukkit.Location loc = target.getLocation();
            replayManager.startRecording(
                    target.getUniqueId(),
                    target.getEntityId(),
                    target.getName(),
                    target.getName() + "_" + System.currentTimeMillis(),
                    loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()
            );
        }

        String success = context.getLang().getMessage("reports.submitted",
                "%target%", target.getName(),
                "%reason%", reason,
                "%server%", serverName);
        reporter.sendMessage(success);

        String notifyMsg = context.getLang().getMessage("reports.staff-notify",
                "%player%", reporter.getName(),
                "%target%", target.getName(),
                "%reason%", reason,
                "%server%", serverName);

        TextComponent clickable = new TextComponent(notifyMsg);
        clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports"));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("lifemod.report.notify")) {
                online.spigot().sendMessage(clickable);
            }
        }

        IMessagingService msgService = ServiceRegistry.get(IMessagingService.class);
        if (msgService != null) {
            String json = "{\"action\":\"CREATE\",\"id\":" + reportId + ",\"reporter\":\"" + reporter.getName() + "\",\"target\":\"" + target.getName() + "\",\"reason\":\"" + reason + "\",\"server\":\"" + serverName + "\"}";
            msgService.publish("lifemod:reports", json);
        }

        WebhookUtil.sendAlert(context, "report");
    }
}
