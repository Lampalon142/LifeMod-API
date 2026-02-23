package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportStatus;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

        if (context.getPlugin().getConfigConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }
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

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
