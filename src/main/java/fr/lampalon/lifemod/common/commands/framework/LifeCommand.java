package fr.lampalon.lifemod.common.commands.framework;

import java.util.List;

public abstract class LifeCommand {
    private final String name;
    private final String permission;
    private final boolean playerOnly;

    public LifeCommand(String name, String permission, boolean playerOnly) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
    }

    public abstract void execute(ICommandSender sender, String[] args);

    public List<String> onTabComplete(ICommandSender sender, String[] args) {
        return null;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }
}

