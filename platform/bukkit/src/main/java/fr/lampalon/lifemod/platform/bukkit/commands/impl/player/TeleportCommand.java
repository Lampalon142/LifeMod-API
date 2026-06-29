package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;


import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.BukkitDatabaseUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TeleportCommand extends LifeCommand {

    public TeleportCommand() {
        super("tp", "lifemod.tp", true, "teleport", "tphere");
        setDescription("Teleports yourself or another player to a location or player (online or offline).");
        setUsage("/tp <player> | /tp <player1> <player2> | /tp <x> <y> <z>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length == 1) {
            String targetName = context.getArgs()[0];
            Player onlineTarget = Bukkit.getPlayer(targetName);
            if (onlineTarget != null && onlineTarget.isOnline()) {
                player.teleport(onlineTarget.getLocation());
                context.getSender().sendMessage(context.getLang().getMessage("commands.teleport.success",
                    "%target%", onlineTarget.getName() + " §a(Online)"));
                context.getDebug().log("tp", player.getName() + " teleported to online player " + onlineTarget.getName());
            } else {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (!offlineTarget.hasPlayedBefore()) {
                    context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                    return;
                }
                Location loc = BukkitDatabaseUtil.fromStoredLocation(
                    context.getPlugin().getDatabaseManager().getDatabaseProvider().getCoords(offlineTarget.getUniqueId()));
                if (loc == null) {
                    context.getSender().sendMessage(context.getLang().getMessage("commands.otp.no-position", "%target%", targetName));
                    return;
                }
                player.teleport(loc);
                context.getSender().sendMessage(context.getLang().getMessage("commands.teleport.success",
                    "%target%", targetName + " §c(Offline)"));
                context.getDebug().log("tp", player.getName() + " teleported to offline player " + targetName);
            }
        } else if (context.getArgs().length == 2) {
            Player target1 = Bukkit.getPlayer(context.getArgs()[0]);
            Player target2 = Bukkit.getPlayer(context.getArgs()[1]);

            if (target1 == null || target2 == null) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }

            target1.teleport(target2.getLocation());
            context.getSender().sendMessage(context.getLang().getMessage("commands.teleport.success-other", "%player1%", target1.getName(), "%player2%", target2.getName()));
            context.getDebug().log("tp", player.getName() + " teleported " + target1.getName() + " to " + target2.getName());
        } else if (context.getArgs().length == 3) {
            try {
                double x = parseCoord(context.getArgs()[0], player.getLocation().getX());
                double y = parseCoord(context.getArgs()[1], player.getLocation().getY());
                double z = parseCoord(context.getArgs()[2], player.getLocation().getZ());

                Location targetLocation = new Location(player.getWorld(), x, y, z);
                player.teleport(targetLocation);
                context.getSender().sendMessage(context.getLang().getMessage("commands.teleport.success", "%target%", String.format("%.2f, %.2f, %.2f", x, y, z)));
                context.getDebug().log("tp", player.getName() + " teleported to coordinates " + x + "," + y + "," + z);
            } catch (NumberFormatException e) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.teleport.invalid-coords"));
            }
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("commands.teleport.usage"));
        }

    }

    private double parseCoord(String arg, double current) {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return current;
            return current + Double.parseDouble(arg.substring(1));
        }
        return Double.parseDouble(arg);
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1 || context.getArgs().length == 2) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[context.getArgs().length - 1]);
        }
        return super.onTabComplete(context);
    }
}
