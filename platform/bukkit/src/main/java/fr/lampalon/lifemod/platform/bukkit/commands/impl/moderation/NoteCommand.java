package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import org.bukkit.OfflinePlayer;

public class NoteCommand extends BaseSanctionCommand {
    public NoteCommand() {
        super("note", "lifemod.note", SanctionType.NOTE, "Add a private staff note to a player.", "/note <player> <content>");
    }

    @Override
    protected boolean supportsDuration() {
        return false;
    }

    @Override
    protected void onSanctionApplied(CommandContext context, OfflinePlayer target, Sanction sanction) {
        context.getSender().sendMessage(context.getLang().getMessage("sanctions.note.success", "%player%", target.getName(), "%reason%", sanction.getReason()));
    }
}
