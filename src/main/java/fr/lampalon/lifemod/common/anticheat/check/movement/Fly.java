package fr.lampalon.lifemod.common.anticheat.check.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
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

public class Fly extends AbstractCheck {

    public Fly(IConfigurationService configService) {
        super("Fly", configService);
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;

        // Exclusions
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.getAllowFlight()) return;
        if (p.isInsideVehicle()) return;

        double y = data.getLastY();
        boolean onGround = data.isOnGround();

        if (context instanceof PacketReceiveEvent) {
            PacketReceiveEvent event = (PacketReceiveEvent) context;
            
            // Ignore rotation-only packets for Fly check
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) return;

            if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || 
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                
                WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
                y = wrapper.getLocation().getY();
                onGround = wrapper.isOnGround();
            }
        } else {
            return;
        }

        double deltaY = y - data.getLastY();
        long timeSinceVelocity = System.currentTimeMillis() - data.getLastVelocityTime();
        
        // Ignore check if recently took velocity (KB)
        if (timeSinceVelocity < 1000) {
            data.setLastY(y);
            data.setOnGround(onGround);
            return;
        }

        if (!onGround) {
            data.setAirTicks(data.getAirTicks() + 1);
            data.setGroundTicks(0);

            // 1. Gravity Check
            // Increased buffer to 40 ticks (2 seconds) to avoid FP after climbing
            if (data.getAirTicks() > 40) {
                if (deltaY >= -0.01) {
                    if (!isClimbable(p)) {
                        flag(uuid, data, 0.9, "Gravity (Suspended > 2s)");
                    }
                }
            }
            
            // 2. Impossible Ascension
            // Max jump height is around 1.2 blocks. 
            if (deltaY > 0.6 && data.getAirTicks() > 5 && !isClimbable(p)) {
                flag(uuid, data, 0.95, "Ascension (Moving up without support)");
            }

        } else {
            data.setAirTicks(0);
            data.setGroundTicks(data.getGroundTicks() + 1);
        }

        // Debug for testing
        if (p.hasPermission("lifemod.anticheat.debug")) {
            // p.sendMessage("§8[Fly] §7AirTicks: " + data.getAirTicks() + " dY: " + String.format("%.3f", deltaY) + " Ground: " + onGround);
        }

        data.setLastY(y);
        data.setOnGround(onGround);
        data.setDeltaY(deltaY);
    }

    private boolean isClimbable(Player p) {
        Location loc = p.getLocation();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 1; y++) {
                    Material m = loc.clone().add(x * 0.4, y, z * 0.4).getBlock().getType();
                    if (m == Material.LADDER || m == Material.VINE || m == Material.SCAFFOLDING || 
                        m == Material.TWISTING_VINES || m == Material.WEEPING_VINES) {
                        return true;
                    }
                }
            }
        }
        return false;
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