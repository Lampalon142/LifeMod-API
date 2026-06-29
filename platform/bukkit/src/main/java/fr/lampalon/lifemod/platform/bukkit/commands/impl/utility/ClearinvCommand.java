package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class ClearinvCommand extends LifeCommand {
    public ClearinvCommand() {
        super("clearinv", "lifemod.clearinv", true);
        setDescription("Clears the inventory of a player.");
        setUsage("/clearinv <player>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length > 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.clearinv.usage"));
            return;
        }

        if (context.getArgs().length == 0) {
            player.getInventory().clear();
            context.getSender().sendMessage(context.getLang().getMessage("commands.clearinv.self"));
            context.getDebug().log("clearinv", player.getName() + " cleared their own inventory");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(context.getArgs()[0]);
        if (targetPlayer == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        targetPlayer.getInventory().clear();
        context.getSender().sendMessage(context.getLang().getMessage("commands.clearinv.message", "%target%", targetPlayer.getName()));
        context.getDebug().log("clearinv", player.getName() + " cleared inventory of " + targetPlayer.getName());

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context);
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
