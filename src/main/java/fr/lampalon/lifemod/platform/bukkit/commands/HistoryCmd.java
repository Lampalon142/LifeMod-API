package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.gui.HistoryGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HistoryCmd extends LifeCommand {

    public HistoryCmd() {
        super("history", "lifemod.history", true);
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            String usage = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class).getMessage("gui.history.usage");
            sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(usage));
            return;
        }

        String targetName = args[0];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = offlineTarget.getUniqueId();

        ServiceRegistry.get(ISanctionService.class).getHistory(targetUuid).thenAccept(history -> {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("LifeMod"), () -> {
                new HistoryGui((Player) sender.getHandle(), history, targetName, targetUuid).open();
            });
        });
    }
}

