package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import io.netty.buffer.ByteBuf;

import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

public class ReplayPacketListener implements PacketListener {

    private static final Logger LOGGER = Logger.getLogger("ReplayPacketListener");
    private final ReplayManager replayManager;

    public ReplayPacketListener(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        UUID uuid = event.getUser().getUUID();
        ReplaySession session = replayManager.getSession(uuid);
        
        // Cas 1 : Actions directes du joueur/bot (Input)
        if (session != null && session.isRecording()) {
            byte[] dataBuffer = serializePacket(event.getByteBuf());
            if (dataBuffer != null) {
                session.addFrame(new ReplayFrame(System.currentTimeMillis(), Collections.singletonList(dataBuffer)));
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Cas 2 : Ce que le serveur diffuse sur l'entité (Mouvements, Animations, etc.)
        com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type = event.getPacketType();
        
        int entityId = -1;
        
        // On extrait l'ID de l'entité concernée par le paquet
        if (type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE)) {
            entityId = new WrapperPlayServerEntityRelativeMove(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION)) {
            entityId = new WrapperPlayServerEntityRelativeMoveAndRotation(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_ROTATION)) {
            entityId = new WrapperPlayServerEntityRotation(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_TELEPORT)) {
            entityId = new WrapperPlayServerEntityTeleport(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_ANIMATION)) {
            entityId = new WrapperPlayServerEntityAnimation(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_METADATA)) {
            entityId = new WrapperPlayServerEntityMetadata(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_EQUIPMENT)) {
            entityId = new WrapperPlayServerEntityEquipment(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_VELOCITY)) {
            entityId = new WrapperPlayServerEntityVelocity(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_HEAD_LOOK)) {
            entityId = new WrapperPlayServerEntityHeadLook(event).getEntityId();
        }
        
        if (entityId != -1) {
            ReplaySession session = replayManager.getSessionByEntityId(entityId);
            if (session != null && session.isRecording()) {
                byte[] dataBuffer = serializePacket(event.getByteBuf());
                if (dataBuffer != null) {
                    session.addFrame(new ReplayFrame(System.currentTimeMillis(), Collections.singletonList(dataBuffer)));
                }
            }
        }
    }

    private byte[] serializePacket(Object buffer) {
        if (buffer instanceof ByteBuf) {
            ByteBuf b = (ByteBuf) buffer;
            if (b.readableBytes() <= 0) return null;
            
            byte[] bytes = new byte[b.readableBytes()];
            b.markReaderIndex();
            b.readBytes(bytes);
            b.resetReaderIndex();
            return bytes;
        }
        return null;
    }
}
