package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import org.bukkit.OfflinePlayer;

public class NoteCmd extends BaseSanctionCmd {
    public NoteCmd() {
        super("note", "lifemod.note", SanctionType.NOTE);
    }

    @Override
    protected void onSanctionApplied(ICommandSender sender, OfflinePlayer target, Sanction sanction) {
        sender.sendMessage("&aNote ajoutée pour &e" + target.getName() + " &a: &f" + sanction.getReason());
        // Pas de message au joueur (invisible)
    }
}

