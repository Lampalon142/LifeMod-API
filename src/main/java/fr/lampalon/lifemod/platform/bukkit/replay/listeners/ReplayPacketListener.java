package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import fr.lampalon.lifemod.common.replay.buffer.ReplayBuffer;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;

import java.util.Collections;

/**
 * Listener that intercepts packets for the replay system.
 */
public class ReplayPacketListener extends PacketListenerAbstract {

    private final ReplayBuffer buffer;

    /**
     * Initializes the listener with a buffer.
     * @param buffer The buffer to store captured packets.
     */
    public ReplayPacketListener(ReplayBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Here we intercept outgoing packets to record them
        // E.g., if (event.getPacketType() == PacketType.Play.Server.ENTITY_POSITION)
        
        // This is where we will map captured packets to ReplayFrame in the future.
        // For now, this is a skeleton for the implementation.
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Intercept incoming packets if needed (e.g., player movement)
    }
}
