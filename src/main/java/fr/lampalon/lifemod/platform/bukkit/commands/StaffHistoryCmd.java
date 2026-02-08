package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.Sanction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class StaffHistoryCmd extends LifeCommand {

    public StaffHistoryCmd() {
        super("staffhistory", "lifemod.admin.staffhistory", false);
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        if (args.length < 1) {
            sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("gui.staffhistory.usage", "&cUsage: /staffhistory <moderator>")));
            return;
        }

        String modName = args[0];
        OfflinePlayer mod = Bukkit.getOfflinePlayer(modName);
        UUID modUuid = mod.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(LifeMod.getInstance(), () -> {
            fr.lampalon.lifemod.common.service.ISanctionService sanctionService = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ISanctionService.class);
            java.util.List<Sanction> history = sanctionService.getSanctionsIssuedBy(mod.getName(), modUuid).join();
            
            if (history.isEmpty()) {
                sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("gui.staffhistory.no-sanctions")));
                return;
            }

            Bukkit.getScheduler().runTask(LifeMod.getInstance(), () -> {
                if (sender.getHandle() instanceof org.bukkit.entity.Player) {
                    new fr.lampalon.lifemod.platform.bukkit.gui.StaffHistoryGui((org.bukkit.entity.Player) sender.getHandle(), history, mod.getName(), modUuid).open();
                } else {
                    sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("gui.staffhistory.gui-only", "&cThis GUI command is reserved for players.")));
                }
            });
        });
    }
}
