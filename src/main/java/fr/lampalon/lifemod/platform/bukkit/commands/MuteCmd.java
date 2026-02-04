package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import org.bukkit.OfflinePlayer;

public class MuteCmd extends BaseSanctionCmd {
    public MuteCmd() {
        super("mute", "lifemod.mute", SanctionType.MUTE);
    }

    @Override
    protected void onSanctionApplied(ICommandSender sender, OfflinePlayer target, Sanction sanction) {
        sender.sendMessage("&aLe joueur &e" + target.getName() + " &aa été réduit au silence pour: &f" + sanction.getReason());
        if (target.isOnline()) {
            target.getPlayer().sendMessage("&cVous avez été réduit au silence ! Raison: &f" + sanction.getReason());
        }
    }
}

