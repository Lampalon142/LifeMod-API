package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.model.SanctionType;

public class UnbanCommand extends BaseRevokeCommand {
    public UnbanCommand() {
        super("unban", "lifemod.unban", SanctionType.BAN, "Unbans a player.", "/unban <player> [reason] [-s]");
    }
}
