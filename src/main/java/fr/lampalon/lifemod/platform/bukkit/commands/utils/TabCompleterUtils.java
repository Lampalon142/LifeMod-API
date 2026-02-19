package fr.lampalon.lifemod.platform.bukkit.commands.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TabCompleterUtils {

    /**
     * Filters a list of options based on the last argument provided by the player.
     *
     * @param options The full list of available suggestions.
     * @param lastArg The last argument entered by the player (case-insensitive prefix match).
     * @return A filtered list of suggestions.
     */
    public static List<String> filter(List<String> options, String lastArg) {
        if (options == null || options.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        if (lastArg.isEmpty()) {
            return options;
        }
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lastArg.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Provides a list of online player names, filtered by the given argument.
     *
     * @param arg The argument to filter player names by.
     * @return A filtered list of online player names.
     */
    public static List<String> filterOnlinePlayers(String arg) {
        return filter(Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList()), arg);
    }

    // Add more utility methods as needed, e.g., for enums, specific contexts, etc.
}
