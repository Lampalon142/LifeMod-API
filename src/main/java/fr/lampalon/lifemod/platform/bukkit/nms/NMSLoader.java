package fr.lampalon.lifemod.platform.bukkit.nms;

import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.nms.v1_20_R1.NMSHandler_v1_20_R1;
import fr.lampalon.lifemod.platform.bukkit.nms.v1_21_R1.NMSHandler_v1_21_R1;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

/**
 * Factory class to load the appropriate NMSProvider based on server version.
 */
public class NMSLoader {

    public static NMSProvider load(Logger logger) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        logger.info("Detecting NMS version: " + version);

        switch (version) {
            case "v1_20_R1":
            case "v1_20_R2":
            case "v1_20_R3":
                return new NMSHandler_v1_20_R1();
            case "v1_21_R1":
            case "v1_21_R2":
            case "v1_21_R3":
                return new NMSHandler_v1_21_R1();
            default:
                // Try to fallback to 1.21 if version is unknown or higher, 
                // since PacketEvents might still handle it.
                logger.warning("Unsupported or unknown NMS version: " + version + ". Attempting fallback to v1_21_R1.");
                return new NMSHandler_v1_21_R1();
        }
    }
}
