package fr.lampalon.lifemod.platform.bukkit.commands.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TabCompleterUtils {

    public static List<String> filter(List<String> options, String lastArg) {
        if (options == null || options.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        if (lastArg == null || lastArg.isEmpty()) {
            return options;
        }
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lastArg.toLowerCase()))
                .collect(Collectors.toList());
    }

    public static List<String> filterOnlinePlayers(String arg) {
        return filter(Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList()), arg);
    }
}
