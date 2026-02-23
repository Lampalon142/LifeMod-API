package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import org.bukkit.OfflinePlayer;

public class MuteCommand extends BaseSanctionCommand {
    public MuteCommand() {
        super("mute", "lifemod.mute", SanctionType.MUTE, "Mutes a player in the chat.", "/mute <player> <reason> [duration] [-s]");
    }

    @Override
    protected void onSanctionApplied(CommandContext context, OfflinePlayer target, Sanction sanction) {
        String success = context.getLang().getMessage("sanctions.mute.success", "%player%", target.getName(), "%reason%", sanction.getReason());
        context.getSender().sendMessage(success);
        
        if (target.isOnline()) {
            String received = context.getLang().getMessage("sanctions.mute.received").replace("%reason%", sanction.getReason());
            target.getPlayer().sendMessage(received);

            context.getPlugin().getPacketController().sendTitle(target.getPlayer(), context.getLang().getMessage("sanctions.mute.title"), 
                context.getLang().getMessage("sanctions.mute.subtitle").replace("%reason%", sanction.getReason()), 10, 40, 10);
            context.getPlugin().getPacketController().sendActionBar(target.getPlayer(), received);
        }
    }
}
