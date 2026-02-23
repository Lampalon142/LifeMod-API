package fr.lampalon.lifemod.common.anticheat.check.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.anticheat.utils.MathUtils;
import fr.lampalon.lifemod.common.service.IConfigurationService;

import java.util.UUID;

/**
 * AutoClicker check using statistical distribution analysis.
 */
public class AutoClicker extends AbstractCheck {

    public AutoClicker(IConfigurationService configService) {
        super("AutoClicker", configService);
    }

    @Override
    public boolean isAsync() {
        return true; 
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{
            PacketType.Play.Client.ANIMATION,
            PacketType.Play.Client.INTERACT_ENTITY
        };
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        if (!(context instanceof PacketReceiveEvent)) return;
        PacketReceiveEvent event = (PacketReceiveEvent) context;

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION || 
            (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY && 
             new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity(event).getAction() == com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction.ATTACK)) {
            
            long now = System.currentTimeMillis();
            long lastSwingTime = data.getLastSwingTime();
            
            if (lastSwingTime != -1) {
                long interval = now - lastSwingTime;
                
                if (interval < 500 && interval > 0) {
                    data.getClickIntervals().add(interval);
                    
                    int minSamples = configService.getInt("anticheat.checks.autoclicker.sample_size", 50);
                    if (data.getClickIntervals().size() >= minSamples) {
                        runAnalysis(uuid, data);
                    }
                }
            }
            data.setLastSwingTime(now);
        }
    }

    private void runAnalysis(UUID uuid, ACPlayerData data) {
        double stdDev = MathUtils.getStandardDeviation(data.getClickIntervals());
        double entropy = MathUtils.getEntropy(data.getClickIntervals());
        
        double minDeviation = configService.getDouble("anticheat.checks.autoclicker.min_deviation", 1.2);
        double minEntropy = configService.getDouble("anticheat.checks.autoclicker.min_entropy", 0.5);

        if (stdDev < minDeviation) {
            double probability = 1.0 - (stdDev / minDeviation);
            flag(uuid, data, probability, "Consistency: " + String.format("%.2f", stdDev));
            data.getClickIntervals().clear();
        } else if (entropy < minEntropy) {
            double probability = 1.0 - (entropy / minEntropy);
            flag(uuid, data, probability, "Entropy: " + String.format("%.2f", entropy));
            data.getClickIntervals().clear();
        }
    }
}
