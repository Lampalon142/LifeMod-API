package fr.lampalon.lifemod.common.anticheat.check.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Timer check measuring the number of incoming movement/flying packets per second.
 */
public class Timer extends AbstractCheck {

    private long lastResetTime = -1;
    private final AtomicInteger packetCount = new AtomicInteger(0);

    public Timer(IConfigurationService configService) {
        super("Timer", configService);
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{
            PacketType.Play.Client.PLAYER_POSITION, 
            PacketType.Play.Client.PLAYER_ROTATION, 
            PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
        };
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        if (!(context instanceof PacketReceiveEvent)) return;
        PacketReceiveEvent event = (PacketReceiveEvent) context;
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.PLAYER_POSITION || 
            type == PacketType.Play.Client.PLAYER_ROTATION || 
            type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            
            long now = System.currentTimeMillis();
            
            if (lastResetTime == -1) {
                lastResetTime = now;
                return;
            }

            packetCount.incrementAndGet();

            if (now - lastResetTime >= 1000) {
                int count = packetCount.get();
                if (count > 22) {
                    double probability = (count - 20) / 10.0;
                    flag(uuid, data, Math.min(1.0, probability), "PPS: " + count);
                }
                
                packetCount.set(0);
                lastResetTime = now;
            }
        }
    }
}
