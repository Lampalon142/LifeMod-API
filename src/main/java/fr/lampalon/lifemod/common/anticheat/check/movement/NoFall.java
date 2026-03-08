package fr.lampalon.lifemod.common.anticheat.check.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

public class NoFall extends AbstractCheck {

    public NoFall(IConfigurationService configService) {
        super("NoFall", configService);
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;

        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.getAllowFlight()) return;

        if (!(context instanceof PacketReceiveEvent)) return;
        PacketReceiveEvent event = (PacketReceiveEvent) context;
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) return;

        WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
        boolean clientOnGround = wrapper.isOnGround();
        double y = wrapper.getLocation().getY();
        double lastY = data.getLastY();
        double deltaY = y - lastY;

        // Tracker for fall distance server-side
        if (deltaY < 0) {
            data.setFallDistance(data.getFallDistance() + (float) Math.abs(deltaY));
        }

        // 1. Ground Spoof Detection (More robust)
        // If client says ground=true, verify there is actually a block below
        if (clientOnGround) {
            if (data.getAirTicks() > 3 && deltaY < 0) {
                // If falling and claims ground, check distance to real ground
                double distToGround = getDistanceToGround(p.getLocation());
                if (distToGround > 1.5) {
                    flag(uuid, data, 0.95, "Ground Spoof (Client says onGround, but air is " + String.format("%.2f", distToGround) + "m)");
                }
            }
            
            // 2. Fall Distance Reset (Catch packets that reset fall distance in air)
            if (data.getFallDistance() > 3.0 && data.getAirTicks() > 8) {
                 // Player fell > 3 blocks and is still in air, but resets fall distance by spoofing ground
                 flag(uuid, data, 0.9, "Packet NoFall (Distance: " + String.format("%.2f", data.getFallDistance()) + ")");
            }
            
            data.setFallDistance(0);
        }
    }

    private double getDistanceToGround(Location loc) {
        double y = loc.getY();
        for (double dy = 0; dy <= 5.0; dy += 1.0) {
            Material m = loc.clone().subtract(0, dy, 0).getBlock().getType();
            if (m.isSolid()) return dy;
        }
        return 10.0;
    }

    private boolean isInLiquid(Player p) {
        Material m = p.getLocation().getBlock().getType();
        return m == Material.WATER || m == Material.LAVA || m == Material.COBWEB || m == Material.LADDER || m == Material.VINE;
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{
                PacketType.Play.Client.PLAYER_POSITION,
                PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
                PacketType.Play.Client.PLAYER_FLYING
        };
    }
}