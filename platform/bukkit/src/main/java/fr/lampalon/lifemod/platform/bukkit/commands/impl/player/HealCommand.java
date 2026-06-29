package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class HealCommand extends LifeCommand {
    public HealCommand() {
        super("heal", "lifemod.heal", true);
        setDescription("Heals a player, restoring their health and hunger.");
        setUsage("/heal [player]");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length == 0) {
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.heal.player"));
            context.getDebug().log("heal", player.getName() + " healed himself");
        } else if (context.getArgs().length == 1) {
            Player target = Bukkit.getPlayer(context.getArgs()[0]);
            if (target != null) {
                target.setHealth(target.getMaxHealth());
                target.setFoodLevel(20);
                context.getSender().sendMessage(context.getLang().getMessage("commands.utility.heal.mod", "%player%", target.getName()));
                target.sendMessage(context.getLang().getMessage("commands.utility.heal.player"));
                context.getDebug().log("heal", player.getName() + " healed " + target.getName());
            } else {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            }
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.heal.usage"));
        }

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
