package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.replay.ReplayCodec;
import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Magic byte scheme:
 *   0xFE = position packet (ReplayPositionRecorder)
 *   0xFD = world packet → sent as-is
 *   0xFC = entity packet → entity ID rewritten via mapper on playback
 *   0xFB = arm-swing (from client)
 *   0xFA = undo block state (ReplayBlockListener)
 *   0xF9 = block change (ReplayBlockListener)
 *   0xDD = spawn entity (non-player, masked when mask-entities=true)
 *   0xDC = destroy entities
 *   0xDE = spawn player (recorded nearby player within radius)
 *   0xF1-F8 = event frames (ReplayEventRecorder)
 */
public class ReplayPacketListener implements PacketListener {

    private static final Logger LOGGER = Logger.getLogger("ReplayPacketListener");

    private static final double MOVEMENT_THRESHOLD = 0.1;

    private final ReplayManager replayManager;

    public ReplayPacketListener(ReplayManager replayManager) {
        if (replayManager == null) throw new IllegalArgumentException("replayManager cannot be null");
        this.replayManager = replayManager;
    }

    // -------------------------------------------------------------------------
    // Incoming — arm swing
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
                        Collections.singletonList(new byte[]{ReplayCodec.ARM_SWING})
                ));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Outgoing
    // -------------------------------------------------------------------------

    @Override
    public void onPacketSend(PacketSendEvent event) {
        com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type = event.getPacketType();

        // ── Skin caching ──────────────────────────────────────────────────────
        if (type.equals(PacketType.Play.Server.PLAYER_INFO)) {
            handlePlayerInfo(event);
            return;
        }

        // ── SPAWN_PLAYER (nearby players → recorded as separate NPCs) ──────────
        if (type.equals(PacketType.Play.Server.SPAWN_PLAYER)) {
            handleSpawnPlayer(event);
            return;
        }

        // ── SPAWN_ENTITY (non-players, masked when mask-entities=true) ─────────
        if (type.equals(PacketType.Play.Server.SPAWN_ENTITY)) {
            handleSpawnEntity(event);
            return;
        }

        // ── DESTROY_ENTITIES ──────────────────────────────────────────────────
        if (type.equals(PacketType.Play.Server.DESTROY_ENTITIES)) {
            handleDestroyEntities(event);
            return;
        }

        // ── Entity movement (throttled: only every 10th tick) ─────────────────
        if (isMovementPacket(type)) {
            handleMovement(event, type);
            return;
        }

        // ── World packets ─────────────────────────────────────────────────────
        if (type.equals(PacketType.Play.Server.BLOCK_CHANGE)
                || type.equals(PacketType.Play.Server.MULTI_BLOCK_CHANGE)
                || type.equals(PacketType.Play.Server.NAMED_SOUND_EFFECT)
                || type.equals(PacketType.Play.Server.BLOCK_BREAK_ANIMATION)) {
            queueForReceiver(event, ReplayCodec.WORLD_PACKET);
            return;
        }

        // ── ENTITY_EQUIPMENT ──────────────────────────────────────────────────
        if (type.equals(PacketType.Play.Server.ENTITY_EQUIPMENT)) {
            handleEquipment(event);
            return;
        }

        // ── Other entity packets (animation, metadata, status, effect) ────────
        int eId = -1;

        if (type.equals(PacketType.Play.Server.ENTITY_ANIMATION)) {
            eId = new WrapperPlayServerEntityAnimation(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_METADATA)) {
            eId = new WrapperPlayServerEntityMetadata(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_STATUS)) {
            eId = new WrapperPlayServerEntityStatus(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.ENTITY_EFFECT)) {
            eId = new WrapperPlayServerEntityEffect(event).getEntityId();
        } else if (type.equals(PacketType.Play.Server.REMOVE_ENTITY_EFFECT)) {
            eId = new WrapperPlayServerRemoveEntityEffect(event).getEntityId();
        }

        if (eId != -1) {
            queueEntityPacket(event, eId);
        }
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    private double recordRadius() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        return cfg != null ? cfg.getDouble("modules.replay.record-radius", 300.0) : 300.0;
    }

    private boolean maskEntities() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        return cfg != null ? cfg.getBoolean("modules.replay.mask-entities", true) : true;
    }

    private boolean recordNearbyPlayers() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        return cfg != null ? cfg.getBoolean("modules.replay.record-nearby-players", true) : true;
    }

    private boolean recordEquipment() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        return cfg != null ? cfg.getBoolean("modules.replay.record-equipment", true) : true;
    }

    private void handlePlayerInfo(PacketSendEvent event) {
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

    private void handleSpawnEntity(PacketSendEvent event) {
        if (maskEntities()) return; // hide non-player entities (zombies, mobs, ...) in replays
        WrapperPlayServerSpawnEntity spawn = readPacket(event, WrapperPlayServerSpawnEntity::new);
        int eId = spawn.getEntityId();

        if (event.getPlayer() instanceof org.bukkit.entity.Player receiver) {
            ReplaySession recvSession = replayManager.getSession(receiver.getUniqueId());
            if (recvSession != null && recvSession.isRecording()) {
                recvSession.trackSpawn(eId);
                recvSession.queuePacket(serializeWithMagic(event.getByteBuf(), ReplayCodec.SPAWN_ENTITY));
            }
        }

        ReplaySession entitySession = replayManager.getSessionByEntityId(eId);
        if (entitySession != null && entitySession.isRecording()) {
            entitySession.trackSpawn(eId);
        }
    }

    private void handleSpawnPlayer(PacketSendEvent event) {
        if (!recordNearbyPlayers()) return;
        WrapperPlayServerSpawnPlayer spawn = readPacket(event, WrapperPlayServerSpawnPlayer::new);
        int eId = spawn.getEntityId();
        UUID uuid = spawn.getUUID();
        if (uuid == null) return;
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player receiver)) return;
        ReplaySession recvSession = replayManager.getSession(receiver.getUniqueId());
        if (recvSession == null || !recvSession.isRecording()) return;

        org.bukkit.entity.Player realNearby = org.bukkit.Bukkit.getPlayer(uuid);
        if (realNearby == null) return;
        if (realNearby.getLocation().distance(receiver.getLocation()) > recordRadius()) return;

        if (recvSession.isRecordable(eId)) return;

        com.github.retrooper.packetevents.util.Vector3d pos = spawn.getPosition();
        String name = realNearby.getName();

        recvSession.trackSpawn(eId);
        recvSession.trackRecordablePlayer(eId);

        byte[] frame;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeByte(ReplayCodec.SPAWN_PLAYER);
            dos.writeInt(eId);
            dos.writeLong(uuid.getMostSignificantBits());
            dos.writeLong(uuid.getLeastSignificantBits());
            dos.writeUTF(name);
            dos.writeDouble(pos.x);
            dos.writeDouble(pos.y);
            dos.writeDouble(pos.z);
            dos.writeFloat(spawn.getYaw());
            dos.writeFloat(spawn.getPitch());
            frame = baos.toByteArray();
        } catch (Exception ignored) {
            return;
        }

        recvSession.queuePacket(frame);
        ReplaySession spawnedSession = replayManager.getSessionByEntityId(eId);
        if (spawnedSession != null && spawnedSession.isRecording() && spawnedSession.getEntityId() != eId) {
            spawnedSession.queuePacket(frame);
        }
    }

    private void handleDestroyEntities(PacketSendEvent event) {
        WrapperPlayServerDestroyEntities destroy = readPacket(event, WrapperPlayServerDestroyEntities::new);
        if (event.getPlayer() instanceof org.bukkit.entity.Player receiver) {
            ReplaySession recvSession = replayManager.getSession(receiver.getUniqueId());
            if (recvSession != null && recvSession.isRecording()) {
                boolean anyActive = false;
                for (int id : destroy.getEntityIds()) {
                    if (recvSession.isEntityActive(id)) anyActive = true;
                    recvSession.trackDestroy(id);
                    recvSession.untrackRecordablePlayer(id);
                }
                if (anyActive) {
                    recvSession.queuePacket(serializeWithMagic(event.getByteBuf(), ReplayCodec.DESTROY_ENTITIES));
                }
            }
        }
    }

    private boolean isMovementPacket(com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type) {
        return type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE)
                || type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION)
                || type.equals(PacketType.Play.Server.ENTITY_ROTATION)
                || type.equals(PacketType.Play.Server.ENTITY_TELEPORT);
    }

    private void handleMovement(PacketSendEvent event, com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player receiver)) return;
        ReplaySession session = replayManager.getSession(receiver.getUniqueId());
        if (session == null || !session.isRecording()) return;

        // Throttle: only record movement every MOVEMENT_INTERVAL ticks
        if (!session.isMovementTick()) return;

        int eId = readEntityId((ByteBuf) event.getByteBuf());

        // Skip recorded player (handled by 0xFE)
        if (eId == session.getEntityId()) return;

        // Skip if entity wasn't tracked (never spawned / already destroyed)
        if (!session.isEntityActive(eId)) return;

        // Dedup: max 1 movement per entity per frame
        if (!session.markMoved(eId)) return;

        // Delta threshold for relative moves
        if (type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE)
                || type.equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION)) {
            if (!isSignificant(event)) return;
        }

            session.queuePacket(serializeWithMagic(event.getByteBuf(), ReplayCodec.ENTITY_PACKET));
    }

    private boolean isSignificant(PacketSendEvent event) {
        ByteBuf b = (ByteBuf) event.getByteBuf();
        int saved = b.readerIndex();
        try {
            double dx, dy, dz;
            if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE)) {
                WrapperPlayServerEntityRelativeMove move = new WrapperPlayServerEntityRelativeMove(event);
                dx = move.getDeltaX(); dy = move.getDeltaY(); dz = move.getDeltaZ();
            } else {
                WrapperPlayServerEntityRelativeMoveAndRotation move = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
                dx = move.getDeltaX(); dy = move.getDeltaY(); dz = move.getDeltaZ();
            }
            return Math.sqrt(dx * dx + dy * dy + dz * dz) >= MOVEMENT_THRESHOLD;
        } catch (Exception e) {
            return true;
        } finally {
            b.readerIndex(saved);
        }
    }

    private void handleEquipment(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player receiver)) return;
        ReplaySession session = replayManager.getSession(receiver.getUniqueId());
        if (session == null || !session.isRecording()) return;
        if (!recordEquipment()) return;
        int equipEntityId = readPacket(event, WrapperPlayServerEntityEquipment::new).getEntityId();
        if (!session.isRecordable(equipEntityId)) return;

        queueForReceiver(event, ReplayCodec.ENTITY_PACKET);

        ReplaySession equipSession = replayManager.getSessionByEntityId(equipEntityId);
        if (equipSession != null && equipSession.isRecording()) {
            if (!receiver.getUniqueId().equals(equipSession.getPlayerUUID())) {
                equipSession.queuePacket(serializeWithMagic(event.getByteBuf(), ReplayCodec.ENTITY_PACKET));
            }
        }
    }

    private void queueEntityPacket(PacketSendEvent event, int entityId) {
        ReplaySession session = replayManager.getSessionByEntityId(entityId);
        if (session != null && session.isRecording()) {
        session.queuePacket(serializeWithMagic(event.getByteBuf(), ReplayCodec.ENTITY_PACKET));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void queueForReceiver(PacketSendEvent event, byte magic) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player receiver = (org.bukkit.entity.Player) event.getPlayer();
        ReplaySession session = replayManager.getSession(receiver.getUniqueId());
        if (session != null && session.isRecording()) {
            session.queuePacket(serializeWithMagic(event.getByteBuf(), magic));
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

    /**
     * Reads entity ID from a packet buffer by skipping the packet ID VarInt first.
     */
    private int readEntityId(ByteBuf buf) {
        int savedIndex = buf.readerIndex();
        try {
            buf.readerIndex(0);
            ReplayCodec.readVarInt(buf); // skip packet ID
            return ReplayCodec.readVarInt(buf); // read entity ID
        } finally {
            buf.readerIndex(savedIndex);
        }
    }

    /**
     * Reads a wrapper from the live packet buffer and restores the reader index
     * afterwards, so the packet sent to the client is not truncated/corrupted.
     */
    private <T> T readPacket(PacketSendEvent event, Function<PacketSendEvent, T> reader) {
        ByteBuf b = (ByteBuf) event.getByteBuf();
        int saved = b.readerIndex();
        try {
            return reader.apply(event);
        } finally {
            b.readerIndex(saved);
        }
    }
}
