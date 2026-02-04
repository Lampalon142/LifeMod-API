package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.model.SanctionType;

public class UnbanCmd extends BaseRevokeCmd {
    public UnbanCmd() {
        super("unban", "lifemod.unban", SanctionType.BAN);
    }
}

