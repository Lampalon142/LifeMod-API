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
import java.util.logging.Logger;

/**
 * Intercepts outgoing packets to record them into the active ReplaySession.
 *
 * Magic byte scheme:
 *   0xFE = custom position packet (ReplayPositionRecorder)
 *   0xFD = world packet  → send as-is (block change, spawn, sound…)
 *   0xFC = entity packet → rewrite originalEntityId → virtualEntityId
 *   0xFB = self arm-swing (from incoming client packet)
 *   0xFA = undo block state (ReplayBlockListener)
 */
public class ReplayPacketListener implements PacketListener {

    private static final Logger LOGGER = Logger.getLogger("ReplayPacketListener");

    public static final byte MAGIC_WORLD_PACKET  = (byte) 0xFD;
    public static final byte MAGIC_ENTITY_PACKET = (byte) 0xFC;

    private final ReplayManager replayManager;

    public ReplayPacketListener(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    // -------------------------------------------------------------------------
    // Incoming
    // -------------------------------------------------------------------------

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Client.ANIMATION)) {
            if (!(event.getPlayer() instanceof org.bukkit.entity.Player)) return;
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getPlayer();
            ReplaySession session = replayManager.getSession(player.getUniqueId());
            if (session != null && session.isRecording()) {
                session.addFrame(new ReplayFrame(
                        System.currentTimeMillis(),
                        Collections.singletonList(new byte[]{(byte) 0xFB})
                ));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Outgoing
    // -------------------------------------------------------------------------

    @Override
    public void onPacketSend(PacketSendEvent event) {
        com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type =
                event.getPacketType();

        // ── Skin caching ──────────────────────────────────────────────────────
        if (type.equals(PacketType.Play.Server.PLAYER_INFO)) {
            WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo(event);
            if (info.getAction() == WrapperPlayServerPlayerInfo.Action.ADD_PLAYER) {
                for (WrapperPlayServerPlayerInfo.PlayerData data : info.getPlayerDataList()) {
                    if (data.getUserProfile().getTextureProperties() != null) {
                        fr.lampalon.lifemod.platform.bukkit.LifeMod.getInstance()
                                .getReplayManager().getSkinManager().cacheSkin(
                                        data.getUserProfile().getUUID(),
                                        data.getUserProfile().getTextureProperties().toArray(
                                                new com.github.retrooper.packetevents.protocol.player.TextureProperty[0])
                                );
                    }
                }
            }
        }

        // ── Movement — record for OTHER entities, skip only the recorded player ──
        if (type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE)
                || type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION)
                || type.equals(PacketType.Play.Server.ENTITY_ROTATION)
                || type.equals(PacketType.Play.Server.ENTITY_TELEPORT)
                || type.equals(PacketType.Play.Server.ENTITY_VELOCITY)
                || type.equals(PacketType.Play.Server.ENTITY_HEAD_LOOK)) {
            if (!(event.getPlayer() instanceof org.bukkit.entity.Player receiver)) return;
            ReplaySession recvSession = replayManager.getSession(receiver.getUniqueId());
            if (recvSession == null || !recvSession.isRecording()) return;
            int moveEntityId = readPacketEntityId(event.getByteBuf());
            if (moveEntityId == recvSession.getEntityId()) return;
            byte[] data = serializeWithMagic(event.getByteBuf(), MAGIC_ENTITY_PACKET);
            if (data != null) {
                recvSession.addFrame(new ReplayFrame(
                        System.currentTimeMillis(), Collections.singletonList(data)));
            }
            return;
        }

        // ── World packets — send as-is (0xFD) ────────────────────────────────
        if (type.equals(PacketType.Play.Server.BLOCK_CHANGE)
                || type.equals(PacketType.Play.Server.MULTI_BLOCK_CHANGE)
                || type.equals(PacketType.Play.Server.SPAWN_ENTITY)
                || type.equals(PacketType.Play.Server.DESTROY_ENTITIES)
                || type.equals(PacketType.Play.Server.NAMED_SOUND_EFFECT)
                || type.equals(PacketType.Play.Server.BLOCK_BREAK_ANIMATION)) {
            recordForReceiver(event, MAGIC_WORLD_PACKET);
            return;
        }

        // ── ENTITY_EQUIPMENT ──────────────────────────────────────────────────
        // This packet is sent to the player themselves AND to nearby players.
        // We need BOTH strategies:
        //   1. recordForReceiver → catches it when sent to the recorded player themselves
        //      (holds item in hand — the packet the server sends back to the holder)
        //   2. getSessionByEntityId → catches it when sent to OTHER nearby players
        //      (so other players see the item too)
        // Using recordForReceiver as primary since that's what the recorded player receives.
        if (type.equals(PacketType.Play.Server.ENTITY_EQUIPMENT)) {
            // Strategy 1: record for the player who is being recorded (sent to self)
            recordForReceiver(event, MAGIC_ENTITY_PACKET);

            // Strategy 2: also record via entity ID for completeness
            int equipEntityId = new WrapperPlayServerEntityEquipment(event).getEntityId();
            ReplaySession equipSession = replayManager.getSessionByEntityId(equipEntityId);
            if (equipSession != null && equipSession.isRecording()) {
                // Avoid duplicate: only record if the receiver is NOT the recorded player
                if (event.getPlayer() instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player receiver = (org.bukkit.entity.Player) event.getPlayer();
                    if (!receiver.getUniqueId().equals(equipSession.getPlayerUUID())) {
                        byte[] data = serializeWithMagic(event.getByteBuf(), MAGIC_ENTITY_PACKET);
                        if (data != null) {
                            equipSession.addFrame(new ReplayFrame(
                                    System.currentTimeMillis(),
                                    Collections.singletonList(data)
                            ));
                        }
                    }
                }
            }
            return;
        }

        // ── Other entity packets (0xFC) ───────────────────────────────────────
        int entityId = -1;

        if (type.equals(PacketType.Play.Server.ENTITY_ANIMATION)) {
            entityId = new WrapperPlayServerEntityAnimation(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_METADATA)) {
            entityId = new WrapperPlayServerEntityMetadata(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_STATUS)) {
            entityId = new WrapperPlayServerEntityStatus(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_EFFECT)) {
            entityId = new WrapperPlayServerEntityEffect(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.REMOVE_ENTITY_EFFECT)) {
            entityId = new WrapperPlayServerRemoveEntityEffect(event).getEntityId();
        }

        if (entityId != -1) {
            ReplaySession session = replayManager.getSessionByEntityId(entityId);
            if (session != null && session.isRecording()) {
                byte[] data = serializeWithMagic(event.getByteBuf(), MAGIC_ENTITY_PACKET);
                if (data != null) {
                    session.addFrame(new ReplayFrame(
                            System.currentTimeMillis(),
                            Collections.singletonList(data)
                    ));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void recordForReceiver(PacketSendEvent event, byte magic) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player receiver = (org.bukkit.entity.Player) event.getPlayer();
        ReplaySession session = replayManager.getSession(receiver.getUniqueId());
        if (session != null && session.isRecording()) {
            byte[] data = serializeWithMagic(event.getByteBuf(), magic);
            if (data != null) {
                session.addFrame(new ReplayFrame(
                        System.currentTimeMillis(),
                        Collections.singletonList(data)
                ));
            }
        }
    }

    private byte[] serializeWithMagic(Object buffer, byte magic) {
        if (!(buffer instanceof ByteBuf)) return null;
        ByteBuf b = (ByteBuf) buffer;
        int savedIndex = b.readerIndex();
        try {
            b.readerIndex(0);
            int readable = b.readableBytes();
            if (readable <= 0) return null;
            byte[] result = new byte[readable + 1];
            result[0] = magic;
            b.readBytes(result, 1, readable);
            return result;
        } finally {
            b.readerIndex(savedIndex);
        }
    }

    private int readPacketEntityId(ByteBuf buf) {
        int savedIndex = buf.readerIndex();
        try {
            buf.readerIndex(0);
            return readVarInt(buf);
        } finally {
            buf.readerIndex(savedIndex);
        }
    }

    private int readVarInt(ByteBuf buf) {
        int v = 0, s = 0; byte b;
        do { b = buf.readByte(); v |= (b & 0x7F) << s; s += 7; } while ((b & 0x80) != 0);
        return v;
    }
}