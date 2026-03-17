package fr.lampalon.lifemod.platform.bukkit.utils;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.BukkitPlatform;
import org.bukkit.entity.Player;

public class ActionBarUtil {

    public static void sendActionBar(Player player, String message) {
        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
        if (platform instanceof BukkitPlatform) {
            NMSProvider nms = ((BukkitPlatform) platform).getNmsProvider();
            if (nms != null) {
                nms.sendActionBar(player, message);
                return;
            }
        }
        // Fallback
        player.sendMessage(message);
    }
}
