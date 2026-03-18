package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import fr.lampalon.lifemod.common.replay.buffer.ReplayBuffer;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.util.Collections;

public class ReplayPacketListener implements com.github.retrooper.packetevents.event.PacketListener {

    private final ReplayBuffer buffer;

    public ReplayPacketListener(ReplayBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void onPacketSend(com.github.retrooper.packetevents.event.PacketSendEvent event) {
        if (isActionPacket(event.getPacketType())) {
            buffer.addFrame(new ReplayFrame(System.currentTimeMillis(), Collections.singletonList(event.getPacketType())));
        }
    }

    private boolean isActionPacket(com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type) {
        return type.equals(com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.ENTITY_RELATIVE_MOVE) ||
               type.equals(com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.ENTITY_ROTATION) ||
               type.equals(com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) ||
               type.equals(com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.ENTITY_TELEPORT);
    }
}
