package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.gui.StaffHistoryGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public class StaffHistoryCommand extends LifeCommand {

    public StaffHistoryCommand() {
        super("staffhistory", "lifemod.admin.staffhistory", false);
        setDescription("View a moderator's sanction history.");
        setUsage("/staffhistory <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("gui.staffhistory.usage"));
            return;
        }

        String modName = context.getArgs()[0];
        OfflinePlayer mod = Bukkit.getOfflinePlayer(modName);
        UUID modUuid = mod.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), () -> {
            ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
            List<Sanction> history = sanctionService.getSanctionsIssuedBy(mod.getName(), modUuid).join();
            
            if (history.isEmpty()) {
                context.getSender().sendMessage(context.getLang().getMessage("gui.staffhistory.no-sanctions"));
                return;
            }

            Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                if (context.isPlayer()) {
                    new StaffHistoryGui(context.getPlayer(), history, mod.getName(), modUuid).open();
                } else {
                    context.getSender().sendMessage(context.getLang().getMessage("gui.staffhistory.gui-only"));
                }
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
