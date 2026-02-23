package fr.lampalon.lifemod.common.anticheat.check.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Aimbot check analyzing rotation smoothness and second derivative (Acceleration).
 */
public class Aimbot extends AbstractCheck {

    private float lastYaw = 0;
    private float lastPitch = 0;
    private float lastYawDelta = 0;
    private float lastPitchDelta = 0;

    public Aimbot(IConfigurationService configService) {
        super("Aimbot", configService);
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{PacketType.Play.Client.PLAYER_ROTATION, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION};
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        if (!(context instanceof PacketReceiveEvent)) return;
        PacketReceiveEvent event = (PacketReceiveEvent) context;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || 
            event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {

            WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
            float yaw = wrapper.getYaw();
            float pitch = wrapper.getPitch();

            float yawDelta = Math.abs(yaw - lastYaw);
            float pitchDelta = Math.abs(pitch - lastPitch);

            // FALSE POSITIVE PREVENTION: Ignore when vertical movement/jump might be occurring
            // Also ignore if the player is not currently in a fight (last attack time could be tracked)
            // For now, let's use a more permissive threshold for vertical movement.
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && (Math.abs(player.getVelocity().getY()) > 0.05 || !player.isOnGround())) {
                lastYaw = yaw;
                lastPitch = pitch;
                lastYawDelta = yawDelta;
                lastPitchDelta = pitchDelta;
                return;
            }

            if (yawDelta > 0.1 && pitchDelta > 0.1) {
                float yawAccel = Math.abs(yawDelta - lastYawDelta);
                float pitchAccel = Math.abs(pitchDelta - lastPitchDelta);

                // Statistical analysis of smoothness
                if (yawAccel < 0.0001 && pitchAccel < 0.0001) {
                     data.getYawDeltas().add(yawAccel);
                     if (data.getYawDeltas().size() > 15) {
                         double avgAccel = data.getYawDeltas().stream().mapToDouble(Float::doubleValue).average().orElse(1.0);
                         if (avgAccel < 0.00001) {
                             flag(uuid, data, 0.8, "Linear: " + String.format("%.7f", avgAccel));
                             data.getYawDeltas().clear();
                         }
                     }
                }
            }

            lastYaw = yaw;
            lastPitch = pitch;
            lastYawDelta = yawDelta;
            lastPitchDelta = pitchDelta;
        }
    }
}
