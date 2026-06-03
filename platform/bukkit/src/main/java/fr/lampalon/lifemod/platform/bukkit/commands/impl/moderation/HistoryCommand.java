package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.gui.HistoryGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HistoryCommand extends LifeCommand {

    public HistoryCommand() {
        super("history", "lifemod.history", true);
        setDescription("View a player's sanction history.");
        setUsage("/history <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("gui.history.usage"));
            return;
        }

        String targetName = context.getArgs()[0];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = offlineTarget.getUniqueId();

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("target_type", "player");
            props.put("result_count", 0);
            ph.capture("lifemod_history_view", props);
        }

        ServiceRegistry.get(ISanctionService.class).getHistory(targetUuid)
                .thenAccept(history -> {
                    Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                        new HistoryGui(context.getPlayer(), history, targetName, targetUuid).open();
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
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
