package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import fr.lampalon.lifemod.common.replay.buffer.ReplayBuffer;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.util.Collections;

/**
 * Listener that captures essential packets for the replay system.
 */
public class ReplayPacketListener extends PacketListenerAbstract {

    private final ReplayBuffer buffer;

    public ReplayPacketListener(ReplayBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Only capture packets if the player is being recorded (logic to be added in ReplayManager)
        
        if (isActionPacket(event.getPacketType())) {
            // In a real implementation, we would clone the packet to prevent memory issues
            buffer.addFrame(new ReplayFrame(System.currentTimeMillis(), Collections.singletonList(event.getPacket())));
        }
    }

    private boolean isActionPacket(PacketType type) {
        return type == PacketType.Play.Server.ENTITY_POSITION ||
               type == PacketType.Play.Server.ENTITY_ROTATION ||
               type == PacketType.Play.Server.ENTITY_POSITION_AND_ROTATION ||
               type == PacketType.Play.Server.ENTITY_TELEPORT ||
               type == PacketType.Play.Server.ANIMATION;
    }
}
