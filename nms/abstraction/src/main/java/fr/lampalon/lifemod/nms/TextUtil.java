package fr.lampalon.lifemod.nms;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(&#)([A-Fa-f0-9]{6})");
    private static final Pattern RGB_PATTERN = Pattern.compile("rgb\\((\\d{1,3}),(\\d{1,3}),(\\d{1,3})\\)");

    private TextUtil() {
    }

    public static String format(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        message = message.replace("\\n", "\n");

        ILangService lang = ServiceRegistry.get(ILangService.class);
        if (message.contains("%prefix%")) {
            String prefix = lang != null ? lang.getPrefix() : "";
            message = message.replace("%prefix%", prefix);
        }

        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(sb, ChatColor.of("#" + hexMatcher.group(2)).toString());
        }
        hexMatcher.appendTail(sb);
        message = sb.toString();

        Matcher rgbMatcher = RGB_PATTERN.matcher(message);
        sb = new StringBuffer();
        while (rgbMatcher.find()) {
            int r = Integer.parseInt(rgbMatcher.group(1));
            int g = Integer.parseInt(rgbMatcher.group(2));
            int b = Integer.parseInt(rgbMatcher.group(3));
            rgbMatcher.appendReplacement(sb, ChatColor.of(new Color(r, g, b)).toString());
        }
        rgbMatcher.appendTail(sb);
        message = sb.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
