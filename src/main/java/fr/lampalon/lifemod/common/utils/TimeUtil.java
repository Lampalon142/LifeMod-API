package fr.lampalon.lifemod.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdw])");

    public static long parseTime(String input) {
        if (input == null || input.isEmpty() || input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanent")) {
            return 0;
        }

        long totalMillis = 0;
        Matcher matcher = TIME_PATTERN.matcher(input.toLowerCase());
        boolean found = false;

        while (matcher.find()) {
            found = true;
            long value = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).charAt(0);

            switch (unit) {
                case 's': totalMillis += value * 1000; break;
                case 'm': totalMillis += value * 60 * 1000; break;
                case 'h': totalMillis += value * 3600 * 1000; break;
                case 'd': totalMillis += value * 86400 * 1000; break;
                case 'w': totalMillis += value * 7 * 86400 * 1000; break;
            }
        }

        return found ? totalMillis : 0;
    }

    public static String formatTime(long millis) {
        if (millis <= 0) return "Permanent";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        if (weeks > 0) return weeks + "w " + (days % 7) + "d";
        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
