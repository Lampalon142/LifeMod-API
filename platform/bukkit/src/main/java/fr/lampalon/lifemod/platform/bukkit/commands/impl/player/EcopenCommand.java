package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class EcopenCommand extends LifeCommand {
    public EcopenCommand() {
        super("ecopen", "lifemod.ecopen", true);
        setDescription("Opens the Ender Chest of another player.");
        setUsage("/ecopen <player>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();

        if (args.length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.ec.usage"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        if (targetPlayer == player) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.ec.yourself"));
            return;
        }

        player.openInventory(targetPlayer.getEnderChest());
        context.getDebug().log("ecopen", player.getName() + " opened ender chest of " + targetPlayer.getName());

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context, "generic", Map.of("%target%", targetPlayer.getName()));
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
