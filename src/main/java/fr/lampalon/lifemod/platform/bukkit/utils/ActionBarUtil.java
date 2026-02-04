package fr.lampalon.lifemod.platform.bukkit.utils;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.entity.Player;

public class ActionBarUtil {

    public static void sendActionBar(Player player, String message) {
        if (LifeMod.getInstance().getPacketController() != null) {
            LifeMod.getInstance().getPacketController().sendActionBar(player, message);
        } else {
            // Fallback très basique si PacketEvents n'est pas chargé (ne devrait pas arriver)
            player.sendMessage(message);
        }
    }
}
