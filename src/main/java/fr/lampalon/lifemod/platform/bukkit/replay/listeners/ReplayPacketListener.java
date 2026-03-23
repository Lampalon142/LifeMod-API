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
        // We do NOT record input packets anymore because they confuse the playback client.
        // We rely on server-side state (position recorder) and outgoing packets (metadata/animations).
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type = event.getPacketType();
        
        // Capture skins from PlayerInfo packets
        if (type.equals(PacketType.Play.Server.PLAYER_INFO)) {
            WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo(event);
            if (info.getAction() == WrapperPlayServerPlayerInfo.Action.ADD_PLAYER) {
                for (WrapperPlayServerPlayerInfo.PlayerData data : info.getPlayerDataList()) {
                    if (data.getUserProfile().getTextureProperties() != null) {
                        fr.lampalon.lifemod.platform.bukkit.LifeMod.getInstance().getReplayManager().getSkinManager().cacheSkin(
                            data.getUserProfile().getUUID(),
                            data.getUserProfile().getTextureProperties().toArray(new com.github.retrooper.packetevents.protocol.player.TextureProperty[0])
                        );
                    }
                }
            }
        }

        int entityId = -1;
        boolean isMovement = false;
        
        // Check for entity packets that indicate movement
        if (type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE)) {
            entityId = new WrapperPlayServerEntityRelativeMove(event).getEntityId();
            isMovement = true;
        } else if (type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION)) {
            entityId = new WrapperPlayServerEntityRelativeMoveAndRotation(event).getEntityId();
            isMovement = true;
        } else if (type.equals(PacketType.Play.Server.ENTITY_ROTATION)) {
            entityId = new WrapperPlayServerEntityRotation(event).getEntityId();
            isMovement = true;
        } else if (type.equals(PacketType.Play.Server.ENTITY_TELEPORT)) {
            entityId = new WrapperPlayServerEntityTeleport(event).getEntityId();
            isMovement = true;
        } else if (type.equals(PacketType.Play.Server.ENTITY_VELOCITY)) {
            entityId = new WrapperPlayServerEntityVelocity(event).getEntityId();
            isMovement = true;
        } else if (type.equals(PacketType.Play.Server.ENTITY_HEAD_LOOK)) {
            entityId = new WrapperPlayServerEntityHeadLook(event).getEntityId();
            isMovement = true;
        } 
        // Non-movement packets that we WANT to keep (Animations, Metadata, Equipment, Status, Effects)
        else if (type.equals(PacketType.Play.Server.ENTITY_ANIMATION)) {
            entityId = new WrapperPlayServerEntityAnimation(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_METADATA)) {
            entityId = new WrapperPlayServerEntityMetadata(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_EQUIPMENT)) {
            entityId = new WrapperPlayServerEntityEquipment(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_STATUS)) {
            entityId = new WrapperPlayServerEntityStatus(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_EFFECT)) {
            entityId = new WrapperPlayServerEntityEffect(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.REMOVE_ENTITY_EFFECT)) {
            entityId = new WrapperPlayServerRemoveEntityEffect(event).getEntityId();
        }
        
        // If it's a movement packet, we IGNORE it because we have the ReplayPositionRecorder
        if (isMovement) {
            return;
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
            int originalReaderIndex = b.readerIndex();
            try {
                b.readerIndex(0);
                if (b.readableBytes() <= 0) return null;
                
                byte[] bytes = new byte[b.readableBytes()];
                b.readBytes(bytes);
                return bytes;
            } finally {
                b.readerIndex(originalReaderIndex);
            }
        }
        return null;
    }
}
