package fr.lampalon.lifemod.platform.bungee.utils;

import net.md_5.bungee.api.ChatColor;

public class MessageUtil {
    public static String formatMessage(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
