package fr.lampalon.lifemod.common.utils;

public class NetworkUtil {

    public static String getSubnet(String ip) {
        if (ip == null) return null;
        int lastDot = ip.lastIndexOf('.');
        if (lastDot == -1) return ip;
        return ip.substring(0, lastDot);
    }
}
