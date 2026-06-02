package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.model.SanctionType;

public class UnmuteCommand extends BaseRevokeCommand {
    public UnmuteCommand() {
        super("unmute", "lifemod.unmute", SanctionType.MUTE, "Unmutes a player.", "/unmute <player> [reason] [-s]");
    }
}
