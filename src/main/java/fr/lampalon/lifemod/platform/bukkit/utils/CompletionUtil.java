package fr.lampalon.lifemod.platform.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CompletionUtil {

    /**
     * Returns a list of online player names visible to the sender.
     */
    public static List<String> getPlayerNames(CommandSender sender) {
        Player playerSender = (sender instanceof Player) ? (Player) sender : null;
        
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> playerSender == null || playerSender.canSee(p))
                .map(HumanEntity::getName)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of all world names.
     */
    public static List<String> getWorldNames() {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toList());
    }

    /**
     * Filters a given list based on the last argument provided by the user.
     * This is the standardized way to filter tab completions.
     */
    public static List<String> filter(List<String> candidates, String[] args) {
        if (args.length == 0) return candidates;
        String last = args[args.length - 1].toLowerCase();
        return candidates.stream()
                .filter(s -> s.toLowerCase().startsWith(last))
                .sorted()
                .collect(Collectors.toList());
    }
}
