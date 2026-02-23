package fr.lampalon.lifemod.platform.bukkit.commands.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class LifeCommand {
    private final String name;
    private final List<String> aliases;
    private final String permission;
    private final boolean playerOnly;
    private String description = "";
    private String usage = "";

    public LifeCommand(String name, String permission, boolean playerOnly, String... aliases) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.aliases = Arrays.asList(aliases);
    }

    public abstract void execute(CommandContext context);

    public List<String> onTabComplete(CommandContext context) {
        return Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }
}
