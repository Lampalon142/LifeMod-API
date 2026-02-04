package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.model.SanctionType;

public class UnmuteCmd extends BaseRevokeCmd {
    public UnmuteCmd() {
        super("unmute", "lifemod.unmute", SanctionType.MUTE);
    }
}

