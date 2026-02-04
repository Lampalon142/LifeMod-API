package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import org.bukkit.OfflinePlayer;

public class WarnCmd extends BaseSanctionCmd {
    public WarnCmd() {
        super("warn", "lifemod.warn", SanctionType.WARN);
    }

    @Override
    protected void onSanctionApplied(ICommandSender sender, OfflinePlayer target, Sanction sanction) {
        sender.sendMessage("&aAvertissement envoyé à &e" + target.getName() + " &apour: &f" + sanction.getReason());
        if (target.isOnline()) {
            target.getPlayer().sendMessage("&c&lAVERTISSEMENT ! &7Raison: &f" + sanction.getReason());
        }
    }
}

