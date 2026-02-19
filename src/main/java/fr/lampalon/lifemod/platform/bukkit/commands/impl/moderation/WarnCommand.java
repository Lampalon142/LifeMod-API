package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import org.bukkit.OfflinePlayer;

public class WarnCommand extends BaseSanctionCommand {
    public WarnCommand(LifeMod plugin) {
        super("warn", "lifemod.warn", SanctionType.WARN, "Warns a player.", "/warn <player> <reason> [-s]", plugin);
    }

    @Override
    protected void onSanctionApplied(CommandContext context, OfflinePlayer target, Sanction sanction) {
        String success = context.getLang().getMessage("sanctions.warn.success", "%player%", target.getName(), "%reason%", sanction.getReason());
        context.getSender().sendMessage(success);
        
        if (target.isOnline()) {
            String received = context.getLang().getMessage("sanctions.warn.received").replace("%reason%", sanction.getReason());
            target.getPlayer().sendMessage(received);
            
            context.getPlugin().getPacketController().sendTitle(target.getPlayer(), context.getLang().getMessage("sanctions.warn.title"), 
                context.getLang().getMessage("sanctions.warn.subtitle").replace("%reason%", sanction.getReason()), 10, 40, 10);
            context.getPlugin().getPacketController().sendActionBar(target.getPlayer(), received);
        }
    }
}
