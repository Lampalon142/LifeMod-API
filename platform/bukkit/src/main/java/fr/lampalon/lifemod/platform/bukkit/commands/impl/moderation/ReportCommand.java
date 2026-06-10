package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportStatus;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ReportCommand extends LifeCommand {

    public ReportCommand() {
        super("report", "lifemod.report", true);
        setDescription("Report a player for rule violations.");
        setUsage("/report <player> <reason>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 2) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.reports.usage"));
            return;
        }
        Player target = Bukkit.getPlayerExact(context.getArgs()[0]);
        if (target == null || !target.isOnline()) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }
        if (target.getUniqueId().equals(context.getSenderUniqueId())) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.reports.yourself"));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length));
        UUID uuid = UUID.randomUUID();
        long now = System.currentTimeMillis();
        Location location = context.getPlayer().getLocation();

        Report report = new Report(
                uuid,
                context.getSenderUniqueId(),
                target.getUniqueId(),
                reason,
                context.getPlugin().getServer().getName(),
                ReportStatus.OPEN,
                null,
                now,
                now,
                null,
                0,
                null
        );
        report.setLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
        context.getPlugin().getDatabaseManager().getDatabaseProvider().saveReport(report);

        context.getSender().sendMessage(context.getLang().getMessage("commands.reports.submitted", 
            "%target%", target.getName(), 
            "%reason%", reason, 
            "%server%", context.getPlugin().getServer().getName()));

        trackReport(reason != null && !reason.isEmpty());

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context);
        }
    }

    private void trackReport(boolean hasReason) {
        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            java.util.Map<String, Object> props = new java.util.HashMap<>();
            props.put("has_reason", hasReason);
            ph.capture("lifemod_report", props);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.report.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.report.description", "")
                                .replace("%player%", playerName))
                        .setFooter(new WebhookFooter(
                                context.getConfig().getString("modules.discord.alerts.footer.text", ""),
                                context.getConfig().getString("modules.discord.alerts.footer.logo", "")
                                        .replace("%player%", playerName)))
                        .setColor(Color.decode(context.getConfig().getString("modules.discord.alerts.report.color", "#FF0000")).getRGB())
                        .build())
                .build()).whenComplete((result, error) -> {
                    if (error != null) {
                        context.getDebug().log("discord", "Webhook error: " + error.getMessage());
                    }
                });
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
