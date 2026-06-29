package fr.lampalon.lifemod.nms;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PacketPlay {

    private PacketPlay() {
    }

    public static void sendHeadRotation(Player player, int entityId, Location location) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerEntityHeadLook(entityId, location.getYaw()));
    }

    public static void sendTeleport(Player player, int entityId, Location location, boolean onGround) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerEntityTeleport(
                        entityId,
                        new Vector3d(location.getX(), location.getY(), location.getZ()),
                        location.getYaw(), location.getPitch(), onGround
                ));
    }
}
