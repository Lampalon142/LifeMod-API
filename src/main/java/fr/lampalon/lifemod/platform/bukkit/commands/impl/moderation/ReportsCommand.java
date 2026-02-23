package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.gui.ReportMainMenu;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

        if (context.getPlugin().getConfigConfig().getBoolean("discord.enabled")) {
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
                List<Report> reports = context.getPlugin().getDatabaseManager().getDatabaseProvider().getAllReports();
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
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.report.title", ""))
                    .setDescription(context.getConfig().getString("discord.report.description", "").replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("discord.report.footer.title", ""),
                            context.getConfig().getString("discord.report.footer.logo", "").replace("%player%", context.getSender().getName()))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.report.color", "")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
