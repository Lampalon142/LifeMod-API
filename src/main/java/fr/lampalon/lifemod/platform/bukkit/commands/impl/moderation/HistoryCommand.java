package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.gui.HistoryGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class HistoryCommand extends LifeCommand {

    public HistoryCommand() {
        super("history", "lifemod.history", true);
        setDescription("View a player's sanction history.");
        setUsage("/history <player>");
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = context.getArgs();
        
        if (args.length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("gui.history.usage"));
            return;
        }

        String targetName = args[0];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = offlineTarget.getUniqueId();

        if (!context.isPlayer()) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-only"));
            return;
        }

        ServiceRegistry.get(ISanctionService.class).getHistory(targetUuid).thenAccept(history -> {
            Bukkit.getScheduler().runTask(LifeMod.getInstance(), () -> {
                new HistoryGui((Player) context.getSender(), history, targetName, targetUuid).open();
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
