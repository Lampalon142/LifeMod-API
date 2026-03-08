package fr.lampalon.lifemod.common.anticheat.check.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;

import java.util.UUID;

/**
 * Timer check using a balance system.
 */
public class Timer extends AbstractCheck {

    public Timer(IConfigurationService configService) {
        super("Timer", configService);
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{
            PacketType.Play.Client.PLAYER_POSITION, 
            PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
        };
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        if (!(context instanceof PacketReceiveEvent)) return;
        
        long now = System.currentTimeMillis();
        long lastMoveTime = data.getLastMoveTime();

        if (lastMoveTime != -1) {
            long delta = now - lastMoveTime;
            long balance = data.getTimerBalance();
            balance += 50; 
            balance -= delta;
            
            if (balance < -200) balance = -200;
            data.setTimerBalance(balance);

            if (balance > 100) { 
                double probability = balance / 200.0;
                flag(uuid, data, Math.min(1.0, probability), "Balance: " + balance + "ms");
                data.setTimerBalance(0);
            }
        }
        data.setLastMoveTime(now);
    }
}
