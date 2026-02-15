package fr.lampalon.lifemod.common.commands.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class LifeCommand {
    private final String name;
    private final List<String> aliases;
    private final String permission;
    private final boolean playerOnly;

    public LifeCommand(String name, String permission, boolean playerOnly, String... aliases) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.aliases = Arrays.asList(aliases);
    }

    public abstract void execute(ICommandSender sender, String[] args);

    /**
     * Default TabComplete implementation.
     * Can be overridden by subclasses.
     */
    public List<String> onTabComplete(ICommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Helper to filter a list based on the last argument.
     */
    protected List<String> filter(List<String> options, String[] args) {
        if (options == null || options.isEmpty()) return Collections.emptyList();
        if (args.length == 0) return new ArrayList<>(options);
        
        String lastArg = args[args.length - 1].toLowerCase();
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
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
}

