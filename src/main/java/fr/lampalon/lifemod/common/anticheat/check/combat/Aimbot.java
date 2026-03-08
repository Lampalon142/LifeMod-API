package fr.lampalon.lifemod.common.anticheat.check.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.anticheat.utils.MathUtils;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Aimbot extends AbstractCheck {

    public Aimbot(IConfigurationService configService) {
        super("Aimbot", configService);
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;

        // Track target from interact packet
        if (context instanceof PacketReceiveEvent) {
            PacketReceiveEvent event = (PacketReceiveEvent) context;
            if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                    data.setLastTargetId(wrapper.getEntityId());
                    data.setLastAttackTime(System.currentTimeMillis());
                }
            }
        }

        float yaw, pitch;
        boolean hasRot = false;

        if (context instanceof PacketReceiveEvent) {
            PacketReceiveEvent event = (PacketReceiveEvent) context;
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
                WrapperPlayClientPlayerRotation w = new WrapperPlayClientPlayerRotation(event);
                yaw = w.getYaw(); pitch = w.getPitch(); hasRot = true;
            } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                WrapperPlayClientPlayerPositionAndRotation w = new WrapperPlayClientPlayerPositionAndRotation(event);
                yaw = w.getYaw(); pitch = w.getPitch(); hasRot = true;
            } else {
                return;
            }
        } else {
            return;
        }

        if (!hasRot) return;

        float lastYaw = data.getLastYaw();
        float lastPitch = data.getLastPitch();
        float deltaYaw = Math.abs(yaw - lastYaw);
        float deltaPitch = Math.abs(pitch - lastPitch);
        if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;

        if (deltaYaw < 0.01 && deltaPitch < 0.01) {
            data.setLastYaw(yaw);
            data.setLastPitch(pitch);
            return;
        }

        long timeSinceAttack = System.currentTimeMillis() - data.getLastAttackTime();
        boolean inCombat = timeSinceAttack < 1000;

        //if (inCombat) {
            Entity target = null;
            // Find target entity by ID
            for (Entity entity : p.getWorld().getEntities()) {
                if (entity.getEntityId() == data.getLastTargetId()) {
                    target = entity;
                    break;
                }
            }

            if (target != null) {
                Location targetLoc = target.getLocation();
                Location playerLoc = p.getLocation();
                
                // Calculate required rotation to look at target
                Vector dir = targetLoc.toVector().subtract(playerLoc.toVector());
                float expectedYaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
                float expectedPitch = (float) Math.toDegrees(Math.atan2(-dir.getY(), Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ())));
                
                float yawDiff = Math.abs(yaw - expectedYaw) % 360;
                if (yawDiff > 180) yawDiff = 360 - yawDiff;
                
                float pitchDiff = Math.abs(pitch - expectedPitch);

                // 1. Snap-to-Target Check
                // If player makes a huge jump and lands perfectly on target
                if (deltaYaw > 20.0 && yawDiff < 1.0) {
                    data.setAimbotSnapBuffer(data.getAimbotSnapBuffer() + 2.5);
                    if (data.getAimbotSnapBuffer() > 4) {
                        flag(uuid, data, 0.98, "Snap-to-Target (Perfect Lock)");
                        data.setAimbotSnapBuffer(0);
                    }
                } else {
                    data.setAimbotSnapBuffer(Math.max(0, data.getAimbotSnapBuffer() - 0.1));
                }

                // 2. Accuracy Consistency
                // If the player maintains a near-perfect angle during combat
                if (yawDiff < 0.05 && deltaYaw > 0.5) {
                    data.setAimbotSmoothBuffer(data.getAimbotSmoothBuffer() + 1.0);
                    if (data.getAimbotSmoothBuffer() > 15) {
                        flag(uuid, data, 0.9, "Impossible Precision (Sticky Aim)");
                        data.setAimbotSmoothBuffer(0);
                    }
                } else {
                    data.setAimbotSmoothBuffer(Math.max(0, data.getAimbotSmoothBuffer() - 0.2));
                }
            }
            
            // 3. Statistical Analysis (GCD & Entropy)
            double gcd = MathUtils.getGcd(deltaYaw, data.getLastDeltaYaw());
            if (deltaYaw > 0.5 && data.getLastDeltaYaw() > 0.5 && gcd < 0.0001) {
                data.setAimbotJitterBuffer(data.getAimbotJitterBuffer() + 1.0);
                if (data.getAimbotJitterBuffer() > 20) {
                    flag(uuid, data, 0.75, "Invalid Sensitivity Pattern");
                    data.setAimbotJitterBuffer(0);
                }
            }
        //}

        data.setLastDeltaYaw(deltaYaw);
        data.setLastYaw(yaw);
        data.setLastPitch(pitch);
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{
                PacketType.Play.Client.PLAYER_ROTATION,
                PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
                PacketType.Play.Client.INTERACT_ENTITY
        };
    }
}