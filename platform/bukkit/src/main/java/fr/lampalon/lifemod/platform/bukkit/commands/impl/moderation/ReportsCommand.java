package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.gui.ReportMainMenu;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class ReportsCommand extends LifeCommand {

    public ReportsCommand() {
        super("reports", "lifemod.reports", true);
        setDescription("View all active reports.");
        setUsage("/reports [page]");
    }

    @Override
    public void execute(CommandContext context) {
        int page = parsePage(context.getArgs());
        int itemsPerPage = context.getPlugin().getLangConfig().getInt("report.main.items-per-page", 45);

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context);
        }

        fetchAndOpenReports(context, page, itemsPerPage);
    }

    private int parsePage(String[] args) {
        if (args.length < 1) return 0;
        try {
            return Math.max(0, Integer.parseInt(args[0]) - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void fetchAndOpenReports(CommandContext context, int page, int itemsPerPage) {
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), () -> {
            try {
                List<Report> reports = context.getPlugin().getDatabaseManager().getDatabaseProvider().getAllReports(100, 0);
                reports.sort(Comparator.comparingLong(Report::getCreatedAt).reversed());

                Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                    if (reports.isEmpty()) {
                        context.getSender().sendMessage(context.getLang().getMessage("commands.reports.no-reports"));
                        return;
                    }
                    new ReportMainMenu(context.getPlayer(), reports).open();
                });
            } catch (Exception e) {
                context.getDebug().log("report", "Error fetching reports: " + e.getMessage());
            }
        });
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
}
