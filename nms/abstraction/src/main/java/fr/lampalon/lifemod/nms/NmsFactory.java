package fr.lampalon.lifemod.nms;

import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class NmsFactory {

    private static final Map<String, String> VERSION_HANDLERS = new LinkedHashMap<>();

    static {
        VERSION_HANDLERS.put("v1_8_R3", "fr.lampalon.lifemod.nms.v1_8_R3.NmsHandlerImpl");
        VERSION_HANDLERS.put("v1_12_R1", "fr.lampalon.lifemod.nms.v1_12_R1.NmsHandlerImpl");
        VERSION_HANDLERS.put("v1_16_R3", "fr.lampalon.lifemod.nms.v1_16_R3.NmsHandlerImpl");
        VERSION_HANDLERS.put("v1_18_R2", "fr.lampalon.lifemod.nms.v1_18_R2.NmsHandlerImpl");
        VERSION_HANDLERS.put("v1_20_R3", "fr.lampalon.lifemod.nms.v1_20_R3.NmsHandlerImpl");
        VERSION_HANDLERS.put("v1_20_R4", "fr.lampalon.lifemod.nms.v1_20_R4.NmsHandlerImpl");
        VERSION_HANDLERS.put("v1_21_R1", "fr.lampalon.lifemod.nms.v1_21_R1.NmsHandlerImpl");
    }

    private NmsFactory() {
    }

    public static NMSProvider load(Logger logger, Plugin plugin) {
        String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        String version = parts.length > 3 ? parts[3] : "unknown";
        logger.info("Detecting NMS version: " + version);

        String className = VERSION_HANDLERS.get(version);

        if (className == null) {
            String bukkitVersion = Bukkit.getBukkitVersion();
            logger.info("Versioned package not found. Bukkit version: " + bukkitVersion);

            if (bukkitVersion.contains("1.21")) {
                className = VERSION_HANDLERS.get("v1_21_R1");
            } else if (bukkitVersion.contains("1.20")) {
                className = VERSION_HANDLERS.get("v1_20_R3");
            }
        }

        if (className == null) {
            logger.warning("Unsupported NMS version: " + version + ". Falling back to latest.");
            className = VERSION_HANDLERS.values().stream().reduce((first, second) -> second).orElse(null);
        }

        if (className == null) {
            throw new IllegalStateException("No NMS handler found for version: " + version);
        }

        try {
            Class<?> clazz = Class.forName(className);
            return (NMSProvider) clazz.getConstructor(Plugin.class).newInstance(plugin);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate NMS handler: " + className, e);
        }
    }
}
