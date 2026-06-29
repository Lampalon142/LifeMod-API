package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class FeedCommand extends LifeCommand {

    public FeedCommand() {
        super("feed", "lifemod.feed", true);
        setDescription("Feeds a player, restoring their hunger.");
        setUsage("/feed [player]");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length == 0) {
            context.getPlayer().setFoodLevel(20);
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.feed.yourself"));
        } else if (context.getArgs().length == 1) {
            Player target = Bukkit.getPlayer(context.getArgs()[0]);
            if (target == null) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }
            target.setFoodLevel(20);
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.feed.mod", "%target%", target.getName()));
            target.sendMessage(context.getLang().getMessage("commands.utility.feed.player"));
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.feed.usage"));
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
