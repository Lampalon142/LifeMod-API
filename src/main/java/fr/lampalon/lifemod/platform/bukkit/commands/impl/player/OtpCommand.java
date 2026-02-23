package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.utils.BukkitDatabaseUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class OtpCommand extends LifeCommand {

    public OtpCommand() {
        super("otp", "lifemod.otp", true);
        setDescription("Teleport to an offline player's last known location.");
        setUsage("/otp <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.otp.usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(context.getArgs()[0]);
        if (!target.hasPlayedBefore()) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.otp.player-not-found", "%target%", context.getArgs()[0]));
            return;
        }

        Location location = BukkitDatabaseUtil.fromStoredLocation(context.getPlugin().getDatabaseManager().getDatabaseProvider().getCoords(target.getUniqueId()));
        if (location == null) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.otp.no-position", "%target%", context.getArgs()[0]));
            return;
        }

        context.getPlayer().teleport(location);
        context.getSender().sendMessage(context.getLang().getMessage("commands.otp.teleported", "%target%", context.getArgs()[0]));

        if (context.getPlugin().getConfigConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context, context.getArgs()[0]);
        }
    }

    private void sendDiscordAlert(CommandContext context, String targetName) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.otp.title", ""))
                    .setDescription(context.getConfig().getString("discord.otp.description", "")
                            .replace("%player%", context.getSender().getName())
                            .replace("%target%", targetName))
                    .setFooter(context.getConfig().getString("discord.otp.footer.title", ""),
                            context.getConfig().getString("discord.otp.footer.logo", ""))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.otp.color", "")))));
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
