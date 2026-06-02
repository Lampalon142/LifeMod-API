package fr.lampalon.lifemod.platform.bukkit.commands.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabCompleterUtils {

    private static final long REFRESH_INTERVAL_MS = 1000;
    private static List<String> cachedNames = new ArrayList<>();
    private static long lastRefresh;

    public static List<String> filter(List<String> options, String lastArg) {
        if (options == null || options.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        if (lastArg == null || lastArg.isEmpty()) {
            return options;
        }
        String lower = lastArg.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    public static List<String> filterOnlinePlayers(String arg) {
        return filter(getCachedPlayerNames(), arg);
    }

    private static List<String> getCachedPlayerNames() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh > REFRESH_INTERVAL_MS) {
            List<String> fresh = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                fresh.add(player.getName());
            }
            cachedNames = fresh;
            lastRefresh = now;
        }
        return cachedNames;
    }
}
