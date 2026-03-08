package fr.lampalon.lifemod.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {
    public static final Pattern HEX_PATTERN = Pattern.compile("(&#)([A-Fa-f0-9]{6})");
    public static final Pattern RGB_PATTERN = Pattern.compile("rgb\\((\\d{1,3}),(\\d{1,3}),(\\d{1,3})\\)");

    /**
     * Parse les codes couleurs standards (&) en format interne (§)
     */
    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = '§';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    public static String format(String text) {
        if (text == null) return "";
        return translateAlternateColorCodes('&', text);
    }
}


