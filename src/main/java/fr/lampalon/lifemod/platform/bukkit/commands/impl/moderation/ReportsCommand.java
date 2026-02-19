package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
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
    private final LifeMod plugin;

    public ReportsCommand(LifeMod plugin) {
        super("reports", "lifemod.reports", true);
        this.plugin = plugin;
        setDescription("View all active reports.");
        setUsage("/reports [page]");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();

        int page = parsePage(args);
        int itemsPerPage = context.getPlugin().getLangConfig().getInt("report.main.items-per-page", 45);

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }

        fetchAndOpenReports(player, page, itemsPerPage, context);
    }

    private int parsePage(String[] args) {
        if (args.length < 1) return 0;
        try {
            return Math.max(0, Integer.parseInt(args[0]) - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void fetchAndOpenReports(Player player, int page, int itemsPerPage, CommandContext context) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<Report> reports = plugin.getDatabaseManager().getDatabaseProvider().getAllReports();
                reports.sort(Comparator.comparingLong(Report::getCreatedAt).reversed());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (reports.isEmpty()) {
                        player.sendMessage(context.getLang().getMessage("commands.reports.no-reports"));
                        return;
                    }
                    new ReportMainMenu(player, reports).open();
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error fetching reports: " + e.getMessage());
                player.sendMessage(context.getLang().getMessage("system.error"));
            }
        });
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.report.title"))
                    .setDescription(context.getConfig().getString("discord.report.description").replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("discord.report.footer.title"),
                            context.getConfig().getString("discord.report.footer.logo").replace("%player%", context.getSender().getName()))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.report.color")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
