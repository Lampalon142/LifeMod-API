package fr.lampalon.lifemod.common.analytics;

import java.nio.charset.StandardCharsets;

public class PostHogObfuscation {

    private static final byte XOR_MASK = 0x5A;

    public static String obfuscate(String input) {
        if (input == null) return null;
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] ^ XOR_MASK);
        }
        return new String(chars);
    }

    public static String deobfuscate(String input) {
        return obfuscate(input);
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java PostHogObfuscation <api_key>");
            return;
        }
        String realKey = args[0];
        String obfuscated = obfuscate(realKey);
        byte[] bytes = obfuscated.getBytes(StandardCharsets.UTF_8);
        String hex = toHex(bytes);
        System.out.println("XOR_MASK: 0x" + Integer.toHexString(XOR_MASK & 0xff));
        System.out.println("OBFUSCATED_KEY_HEX: \"" + hex + "\"");
        String verify = deobfuscate(new String(fromHex(hex), StandardCharsets.UTF_8));
        System.out.println("Match: " + realKey.equals(verify));
    }
}
