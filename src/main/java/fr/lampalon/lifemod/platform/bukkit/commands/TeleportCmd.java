package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.CompletionUtil;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TeleportCmd extends LifeCommand {
    private final DebugManager debug = LifeMod.getInstance().getDebugManager();

    public TeleportCmd() {
        super("teleport", "lifemod.tp", true, "tp", "tphere");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        Player player = (Player) sender.getHandle();

        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(LifeMod.getInstance().getLangConfig().getString("system.player-not-found"));
                debug.log("tp", "Target offline: " + args[0]);
                return;
            }
            player.teleport(target.getLocation());
            sender.sendMessage(LifeMod.getInstance().getLangConfig().getString("commands.teleport.success").replace("%target%", target.getName()));
            debug.log("tp", player.getName() + " teleported to " + target.getName());
        }
        else if (args.length == 2) {
            // Logic for /tp player1 player2 OR /tphere player
            // Check alias usage or args
            // For now assuming standard /tp player target behavior
            Player target1 = Bukkit.getPlayer(args[0]);
            Player target2 = Bukkit.getPlayer(args[1]);
            
            if (target1 == null || target2 == null) {
                sender.sendMessage(LifeMod.getInstance().getLangConfig().getString("system.player-not-found"));
                return;
            }
            
            target1.teleport(target2.getLocation());
            sender.sendMessage(LifeMod.getInstance().getLangConfig().getString("commands.teleport.success-other")
                    .replace("%player1%", target1.getName())
                    .replace("%player2%", target2.getName()));
            debug.log("tp", player.getName() + " teleported " + target1.getName() + " to " + target2.getName());
        }
        else if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                Location targetLocation = new Location(player.getWorld(), x, y, z);
                player.teleport(targetLocation);
                sender.sendMessage(LifeMod.getInstance().getLangConfig().getString("commands.teleport.success").replace("%target%", x + "," + y + "," + z));
                debug.log("tp", player.getName() + " teleported to coordinates " + x + "," + y + "," + z);
            } catch (NumberFormatException e) {
                sender.sendMessage(LifeMod.getInstance().getLangConfig().getString("commands.teleport.invalid-coords"));
                debug.log("tp", player.getName() + " entered invalid coordinates");
            }
        } else {
            sender.sendMessage(LifeMod.getInstance().getLangConfig().getString("commands.teleport.usage"));
        }
    }

    @Override
    public List<String> onTabComplete(ICommandSender sender, String[] args) {
        if (args.length == 1 || args.length == 2) {
            return filter(CompletionUtil.getPlayerNames((CommandSender) sender.getHandle()), args);
        }
        return Collections.emptyList();
    }
}


