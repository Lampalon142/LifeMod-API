package fr.lampalon.lifemod.platform.bukkit.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.common.replay.EntityIdMapper;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.replay.SkinUtil;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;

public class PlaybackManager {
    private static final Logger LOGGER = Logger.getLogger("PlaybackManager");
    private static final int MAX_ENTITY_COUNT = 10000;

    private final LifeMod plugin;
    private List<ReplayFrame> frames;
    private int currentIndex = 0;
    private int virtualEntityId;
    private UUID npcUUID;
    private BukkitTask playbackTask;
    private boolean isPaused = false;
    private boolean playing = false;

    private final EntityIdMapper entityIdMapper = new EntityIdMapper();
    private final Set<Integer> spawnedVirtualIds = new HashSet<>();
    private final Map<Integer, UUID> npcUuids = new HashMap<>();
    private Player spectator;

    public PlaybackManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Start
    // -------------------------------------------------------------------------

    public void startPlayback(Player spectator, List<ReplayFrame> frames, ReplaySession session) {
        if (spectator == null || frames == null || session == null) {
            LOGGER.severe("startPlayback: null argument");
            return;
        }
        startPlayback(spectator, frames,
                session.getEntityId(), session.getPlayerUUID(), session.getPlayerName(),
                new Location(spectator.getWorld(),
                        session.getStartX(), session.getStartY(), session.getStartZ(),
                        session.getStartYaw(), session.getStartPitch()));
    }

    public void startPlayback(Player spectator, List<ReplayFrame> frames,
                              int originalEntityId, UUID targetUUID,
                              String targetName, Location startLoc) {
        if (spectator == null || frames == null || targetUUID == null) {
            LOGGER.severe("startPlayback: null argument (spectator=" + spectator + " frames=" + (frames == null ? "null" : frames.size()) + " targetUUID=" + targetUUID + ")");
            return;
        }
        if (frames.isEmpty()) {
            LOGGER.warning("startPlayback: empty frames list");
            return;
        }
        if (playing) {
            LOGGER.warning("startPlayback: already playing, stopping first");
            stopPlayback();
        }

        this.spectator = spectator;
        this.frames = frames;
        this.currentIndex = 0;
        this.isPaused = false;
        this.playing = true;
        this.npcUUID = UUID.randomUUID();

        entityIdMapper.clear();
        spawnedVirtualIds.clear();
        npcUuids.clear();

        this.virtualEntityId = entityIdMapper.getVirtualId(originalEntityId);
        npcUuids.put(virtualEntityId, npcUUID);

        DebugManager debug = plugin.getDebugManager();
        debug.log("replay", "Starting playback for " + spectator.getName()
                + " | Target: " + targetName
                + " | frames: " + frames.size()
                + " | virtualId: " + virtualEntityId);

        try {
            plugin.getReplayPlayerManager().registerPlayback(spectator, this);
        } catch (Exception e) {
            LOGGER.severe("Failed to register playback: " + e.getMessage());
            return;
        }

        Player realPlayer = targetUUID != null ? Bukkit.getPlayer(targetUUID) : null;
        if (realPlayer != null && realPlayer.isOnline()) {
            try { spectator.hidePlayer(plugin, realPlayer); } catch (Exception ignored) {}
        }

        try {
            spectator.teleport(buildObserverLocation(startLoc));
        } catch (Exception e) {
            LOGGER.warning("Failed to teleport spectator: " + e.getMessage());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!spectator.isOnline()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() { cleanup(spectator, realPlayer); }
                    }.runTask(plugin);
                    return;
                }

                // Fetch skin on background thread (blocks here, not the main thread)
                TextureProperty[] skin = SkinUtil.resolveSkin(
                        plugin.getReplayManager().getSkinManager(), targetUUID, targetName);
                if (skin == null || skin.length == 0) {
                    skin = plugin.getReplayManager()
                            .getSkinManager().getOrFetchSkin(targetUUID);
                }

                TextureProperty[] finalSkin = skin;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!spectator.isOnline()) {
                            cleanup(spectator, realPlayer);
                            return;
                        }
                        try {
                            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
                            NMSProvider nms = platform != null ? platform.getNmsProvider() : null;
                            if (nms == null) {
                                LOGGER.severe("NMSProvider is null, cannot spawn NPC");
                                cleanup(spectator, realPlayer);
                                return;
                            }
                            nms.spawnNPC(spectator, virtualEntityId, npcUUID, targetName, finalSkin, startLoc);

                            restoreBlocks(spectator);
                            startPlaybackLoop(spectator, realPlayer);
                        } catch (Exception e) {
                            LOGGER.severe("Failed to initialize playback: " + e.getMessage());
                            cleanup(spectator, realPlayer);
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    // -------------------------------------------------------------------------
    // Block restore at replay start
    // -------------------------------------------------------------------------

    private void restoreBlocks(Player spectator) {
        applyBlockStates(spectator, collectUndoStates(0));
    }

    /**
     * Collects the latest "BEFORE" (0xFA) block state per position, scanning frames
     * backwards from the end down to {@code fromIndex}. Used both at playback start
     * (entire replay) and when seeking backwards (from the seek target onward).
     */
    private Map<Long, Integer> collectUndoStates(int fromIndex) {
        Map<Long, Integer> toRestore = new HashMap<>();
        if (frames == null) return toRestore;
        try {
            for (int i = frames.size() - 1; i >= fromIndex; i--) {
                ReplayFrame frame = frames.get(i);
                if (frame == null) continue;
                List<byte[]> packets = frame.getPackets();
                if (packets == null) continue;
                for (byte[] data : packets) {
                    if (data == null || data.length < 17 || data[0] != ReplayCodec.BLOCK_UNDO) continue;
                    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
                        dis.readByte();
                        int x = dis.readInt(), y = dis.readInt(), z = dis.readInt();
                        int blockId = dis.readInt();
                        toRestore.put(encodePos(x, y, z), blockId);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            LOGGER.warning("collectUndoStates error: " + e.getMessage());
        }
        return toRestore;
    }

    private void applyBlockStates(Player spectator, Map<Long, Integer> states) {
        if (spectator == null || states == null || states.isEmpty()) return;
        int count = 0;
        for (Map.Entry<Long, Integer> entry : states.entrySet()) {
            sendBlockChange(spectator,
                    decodeX(entry.getKey()), decodeY(entry.getKey()), decodeZ(entry.getKey()), entry.getValue());
            count++;
        }
        ILangService lang = ServiceRegistry.get(ILangService.class);
        if (lang != null) {
            spectator.sendMessage(lang.getMessage("replay.playback.blocks-restored", "%count%", String.valueOf(count)));
        }
    }

    private void sendBlockChange(Player spectator, int x, int y, int z, int blockId) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                    new WrapperPlayServerBlockChange(
                            new com.github.retrooper.packetevents.util.Vector3i(x, y, z), blockId));
        } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // Stop / Pause / Speed / Seek
    // -------------------------------------------------------------------------

    public void stopPlayback() {
        playing = false;
        if (playbackTask != null && !playbackTask.isCancelled()) {
            playbackTask.cancel();
            playbackTask = null;
        }
        // Destroy all spawned entities and remove NPCs
        if (spectator != null) {
            destroyAllSpawnedEntities(spectator);
            entityIdMapper.clear();
            try {
                NMSProvider nms = ServiceRegistry.get(ILifePlatform.class).getNmsProvider();
                if (nms != null) {
                    for (Map.Entry<Integer, UUID> e : npcUuids.entrySet()) {
                        nms.removeNPC(spectator, e.getKey(), e.getValue());
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("stopPlayback cleanup error: " + e.getMessage());
            }
            npcUuids.clear();
        }
    }

    public void setPaused(boolean paused) { this.isPaused = paused; }
    public boolean isPaused() { return isPaused; }
    public int getCurrentIndex() { return currentIndex; }

    public void seekTo(Player spectator, int targetIndex) {
        if (spectator == null || frames == null || frames.isEmpty()) return;
        if (targetIndex < 0) targetIndex = 0;
        if (targetIndex >= frames.size()) targetIndex = frames.size() - 1;
        if (targetIndex == currentIndex) return;

        if (targetIndex < currentIndex) {
            for (Map.Entry<Integer, UUID> e : npcUuids.entrySet()) {
                try {
                    NMSProvider nms = ServiceRegistry.get(ILifePlatform.class).getNmsProvider();
                    if (nms != null) nms.removeNPC(spectator, e.getKey(), e.getValue());
                } catch (Exception ignored) {}
            }
            npcUuids.clear();
            spawnedVirtualIds.clear();
            applyBlockStates(spectator, collectUndoStates(targetIndex));
            currentIndex = 0;
        }

        for (int i = currentIndex; i < targetIndex; i++) {
            ReplayFrame frame = frames.get(i);
            if (frame == null) continue;
            List<byte[]> packets = frame.getPackets();
            if (packets == null) continue;
            for (byte[] data : packets) {
                if (data == null || data.length < 1) continue;
                try { dispatchPacket(spectator, data); } catch (Exception ignored) {}
            }
        }
        this.currentIndex = targetIndex;
    }

    // -------------------------------------------------------------------------
    // Playback loop
    // -------------------------------------------------------------------------

    private void startPlaybackLoop(Player spectator, Player realPlayer) {
        if (!playing) return;
        playbackTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!spectator.isOnline() || !plugin.getReplayPlayerManager().isInReplay(spectator)) {
                    cleanup(spectator, realPlayer);
                    this.cancel();
                    return;
                }
                if (isPaused || !playing) return;
                if (frames == null || currentIndex >= frames.size()) {
                    cleanup(spectator, realPlayer);
                    try { plugin.getReplayPlayerManager().exitReplay(spectator); } catch (Exception ignored) {}
                    ILangService lang = ServiceRegistry.get(ILangService.class);
                    if (lang != null) spectator.sendMessage(lang.getMessage("replay.playback.finished"));
                    this.cancel();
                    return;
                }

                ReplayFrame frame = frames.get(currentIndex);
                if (frame != null) {
                    List<byte[]> packets = frame.getPackets();
                    if (packets != null) {
                        for (byte[] data : packets) {
                            if (data == null || data.length < 1) continue;
                            try { dispatchPacket(spectator, data); } catch (Exception ignored) {}
                        }
                    }
                }
                currentIndex++;

                if (currentIndex % 200 == 0 && frames.size() > 0) {
                    ILangService lang = ServiceRegistry.get(ILangService.class);
                    if (lang != null) {
                        spectator.sendMessage(lang.getMessage("replay.playback.progress",
                                "%percent%", String.valueOf((int)((currentIndex / (double) frames.size()) * 100))));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // -------------------------------------------------------------------------
    // Packet dispatch
    // -------------------------------------------------------------------------

    private void dispatchPacket(Player spectator, byte[] data) throws Exception {
        if (data == null || data.length < 1) return;
        byte magic = data[0];

        if (magic == ReplayCodec.POSITION) {
            dispatchPosition(spectator, data);
            return;
        }
        if (magic == ReplayCodec.ARM_SWING) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                    new WrapperPlayServerEntityAnimation(virtualEntityId,
                            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM));
            return;
        }
        if (magic == ReplayCodec.BLOCK_UNDO) return;

        if (magic == ReplayCodec.BLOCK_CHANGE) {
            dispatchBlockChange(spectator, data);
            return;
        }
        if (magic >= ReplayCodec.EVENT_DROP && magic <= ReplayCodec.EVENT_PICKUP) {
            displayEvent(spectator, data);
            return;
        }

        byte[] raw = new byte[data.length - 1];
        System.arraycopy(data, 1, raw, 0, raw.length);

        if (magic == ReplayCodec.SPAWN_ENTITY) {
            handleSpawnEntity(spectator, raw);
            return;
        }
        if (magic == ReplayCodec.SPAWN_PLAYER) {
            handleSpawnPlayer(spectator, raw);
            return;
        }
        if (magic == ReplayCodec.SPAWN_MOB) {
            handleSpawnMob(spectator, raw);
            return;
        }
        if (magic == ReplayCodec.DESTROY_ENTITIES) {
            handleDestroyEntities(spectator, raw);
            return;
        }
        if (magic == ReplayCodec.WORLD_PACKET) {
            try {
                PacketEvents.getAPI().getPlayerManager().sendPacket(
                        spectator, Unpooled.wrappedBuffer(raw));
            } catch (Exception ignored) {}
            return;
        }
        if (magic == ReplayCodec.ENTITY_PACKET) {
            byte[] rewritten = rewriteEntityId(raw);
            if (rewritten != null) {
                try {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(
                            spectator, Unpooled.wrappedBuffer(rewritten));
                } catch (Exception ignored) {}
            }
            return;
        }
    }

    private void dispatchPosition(Player spectator, byte[] data) throws Exception {
        ReplayCodec.Position p = ReplayCodec.parsePosition(data);
        if (p == null) return;
        PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                new WrapperPlayServerEntityTeleport(virtualEntityId,
                        new Vector3d(p.x, p.y, p.z), p.yaw, p.pitch, p.onGround));
        PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                new WrapperPlayServerEntityHeadLook(virtualEntityId, p.yaw));
    }

    private void dispatchBlockChange(Player spectator, byte[] data) throws Exception {
        if (data.length < 17) return;
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            dis.readByte();
            int x = dis.readInt(), y = dis.readInt(), z = dis.readInt();
            int blockId = dis.readInt();
            sendBlockChange(spectator, x, y, z, blockId);
        }
    }

    // -------------------------------------------------------------------------
    // 0xDD handler
    // -------------------------------------------------------------------------

    private void handleSpawnEntity(Player spectator, byte[] raw) {
        if (raw == null || raw.length < 2) return;
        try {
            ByteBuf buf = Unpooled.wrappedBuffer(raw);
            int packetId = ReplayCodec.readVarInt(buf);
            int realEntityId = ReplayCodec.readVarInt(buf);
            int virtualId = entityIdMapper.getVirtualId(realEntityId);
            if (spawnedVirtualIds.size() >= MAX_ENTITY_COUNT) {
                LOGGER.warning("Too many spawned entities, skipping spawn of " + realEntityId);
                return;
            }
            spawnedVirtualIds.add(virtualId);

            int entityIdPos = ReplayCodec.varIntSize(packetId);
            byte[] rewritten = replaceVarInt(raw, entityIdPos, realEntityId, virtualId);

            PacketEvents.getAPI().getPlayerManager().sendPacket(
                    spectator, Unpooled.wrappedBuffer(rewritten));
        } catch (Exception e) {
            LOGGER.warning("handleSpawnEntity error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 0xDE handler — nearby player NPC
    // -------------------------------------------------------------------------

    private void handleSpawnPlayer(Player spectator, byte[] raw) {
        if (raw == null || raw.length < 2) return;
        try {
            int realEntityId;
            UUID uuid;
            String name;
            double x, y, z;
            float yaw, pitch;
            try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(raw))) {
                realEntityId = dis.readInt();
                uuid = new UUID(dis.readLong(), dis.readLong());
                name = dis.readUTF();
                x = dis.readDouble(); y = dis.readDouble(); z = dis.readDouble();
                yaw = dis.readFloat(); pitch = dis.readFloat();
            }

            int virtualId = entityIdMapper.getVirtualId(realEntityId);
            if (spawnedVirtualIds.size() >= MAX_ENTITY_COUNT) {
                LOGGER.warning("Too many spawned entities, skipping player spawn of " + name);
                return;
            }
            spawnedVirtualIds.add(virtualId);

            TextureProperty[] skin = SkinUtil.resolveSkin(
                    plugin.getReplayManager().getSkinManager(), uuid, name);
            UUID npcUuid = UUID.randomUUID();
            npcUuids.put(virtualId, npcUuid);
            Location loc = new Location(spectator.getWorld(), x, y, z, yaw, pitch);

            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
            NMSProvider nms = platform != null ? platform.getNmsProvider() : null;
            if (nms == null) {
                spawnedVirtualIds.remove(virtualId);
                npcUuids.remove(virtualId);
                return;
            }
            nms.spawnNPC(spectator, virtualId, npcUuid, name, skin, loc);
        } catch (Exception e) {
            LOGGER.warning("handleSpawnPlayer error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 0xDB handler (mob spawn — contextual combat visibility)
    // -------------------------------------------------------------------------

    private void handleSpawnMob(Player spectator, byte[] raw) {
        if (raw == null || raw.length < 9) return;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(raw);
             DataInputStream dis = new DataInputStream(bais)) {
            int realEntityId = dis.readInt();
            String typeName = dis.readUTF();
            double x = dis.readDouble();
            double y = dis.readDouble();
            double z = dis.readDouble();
            float yaw = dis.readFloat();
            float pitch = dis.readFloat();

            int virtualId = entityIdMapper.getVirtualId(realEntityId);
            if (spawnedVirtualIds.contains(virtualId)) return;
            spawnedVirtualIds.add(virtualId);

            Location loc = new Location(spectator.getWorld(), x, y, z, yaw, pitch);
            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
            NMSProvider nms = platform != null ? platform.getNmsProvider() : null;
            if (nms == null) {
                spawnedVirtualIds.remove(virtualId);
                return;
            }
            nms.spawnEntity(spectator, virtualId, typeName, loc);
        } catch (Exception e) {
            LOGGER.warning("handleSpawnMob error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 0xDC handler
    // -------------------------------------------------------------------------

    private void handleDestroyEntities(Player spectator, byte[] raw) {
        if (raw == null || raw.length < 2) return;
        try {
            ByteBuf buf = Unpooled.wrappedBuffer(raw);
            ReplayCodec.readVarInt(buf);
            int count = ReplayCodec.readVarInt(buf);
            if (count <= 0 || count > 1000) {
                LOGGER.warning("Invalid destroy count: " + count);
                return;
            }
            int[] virtualIds = new int[count];
            int valid = 0;
            for (int i = 0; i < count; i++) {
                try {
                    int realId = ReplayCodec.readVarInt(buf);
                    int virtualId = entityIdMapper.getVirtualId(realId);
                    virtualIds[valid++] = virtualId;
                    spawnedVirtualIds.remove(virtualId);
                } catch (Exception e) {
                    LOGGER.warning("Failed to read destroy entity ID at index " + i);
                    break;
                }
            }
            if (valid == 0) return;
            int[] trimmed = valid < count ? java.util.Arrays.copyOf(virtualIds, valid) : virtualIds;
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                    new WrapperPlayServerDestroyEntities(trimmed));
        } catch (Exception e) {
            LOGGER.warning("handleDestroyEntities error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Entity ID rewriting via mapper
    // -------------------------------------------------------------------------

    private byte[] rewriteEntityId(byte[] raw) {
        if (raw == null || raw.length < 2) return null;
        try {
            ByteBuf buf = Unpooled.wrappedBuffer(raw);
            int packetId = ReplayCodec.readVarInt(buf);
            int entityIdPos = ReplayCodec.varIntSize(packetId);
            int realEntityId = ReplayCodec.readVarInt(buf);
            int virtualId = entityIdMapper.getVirtualId(realEntityId);
            if (virtualId == realEntityId) return raw;
            return replaceVarInt(raw, entityIdPos, realEntityId, virtualId);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] replaceVarInt(byte[] data, int offset, int oldValue, int newValue) {
        if (data == null || offset < 0 || offset >= data.length) return data;
        int oldSize = ReplayCodec.varIntSize(oldValue);
        int newSize = ReplayCodec.varIntSize(newValue);
        if (offset + oldSize > data.length) return data;
        ByteBuf out = Unpooled.buffer(data.length - oldSize + newSize);
        out.writeBytes(data, 0, offset);
        ReplayCodec.writeVarInt(out, newValue);
        out.writeBytes(data, offset + oldSize, data.length - offset - oldSize);
        byte[] result = new byte[out.readableBytes()];
        out.readBytes(result);
        return result;
    }

    // -------------------------------------------------------------------------
    // Position encoding
    // -------------------------------------------------------------------------

    private long encodePos(int x, int y, int z) {
        return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (long)(y & 0xFFF);
    }

    private int decodeX(long k) { int x=(int)((k>>38)&0x3FFFFFF); return x>=(1<<25)?x-(1<<26):x; }
    private int decodeY(long k) { int y=(int)(k&0xFFF); return y>=(1<<11)?y-(1<<12):y; }
    private int decodeZ(long k) { int z=(int)((k>>12)&0x3FFFFFF); return z>=(1<<25)?z-(1<<26):z; }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void destroyAllSpawnedEntities(Player spectator) {
        if (spectator == null || spawnedVirtualIds.isEmpty()) return;
        int[] ids;
        synchronized (spawnedVirtualIds) {
            ids = new int[spawnedVirtualIds.size()];
            int i = 0;
            for (Integer id : spawnedVirtualIds) ids[i++] = id;
            spawnedVirtualIds.clear();
        }
        if (ids.length == 0) return;
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                    new WrapperPlayServerDestroyEntities(ids));
        } catch (Exception ignored) {}
    }

    private void cleanup(Player spectator, Player realPlayer) {
        playing = false;
        try {
            destroyAllSpawnedEntities(spectator);
            entityIdMapper.clear();
            NMSProvider nms = ServiceRegistry.get(ILifePlatform.class).getNmsProvider();
            if (nms != null) {
                for (Map.Entry<Integer, UUID> e : npcUuids.entrySet()) {
                    nms.removeNPC(spectator, e.getKey(), e.getValue());
                }
            }
        } catch (Exception e) {
            LOGGER.warning("cleanup error: " + e.getMessage());
        }
        npcUuids.clear();
        if (realPlayer != null && realPlayer.isOnline()) {
            try { spectator.showPlayer(plugin, realPlayer); } catch (Exception ignored) {}
        }
    }

    private Location buildObserverLocation(Location npcLoc) {
        if (npcLoc == null) return null;
        try {
            IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
            double distance = config != null ? config.getDouble("modules.replay.observer-distance", 5.0) : 5.0;
            double yOffset = config != null ? config.getDouble("modules.replay.observer-y-offset", 2.0) : 2.0;
            double yawOffset = config != null ? config.getDouble("modules.replay.observer-yaw-offset", 180.0) : 180.0;
            float pitch = config != null ? (float) config.getDouble("modules.replay.observer-pitch", 15.0) : 15.0f;

            double yawRad = Math.toRadians(npcLoc.getYaw());
            Location obs = npcLoc.clone();
            obs.setX(npcLoc.getX() + Math.sin(yawRad) * distance);
            obs.setY(npcLoc.getY() + yOffset);
            obs.setZ(npcLoc.getZ() - Math.cos(yawRad) * distance);
            obs.setYaw(npcLoc.getYaw() + (float) yawOffset);
            obs.setPitch(pitch);
            return obs;
        } catch (Exception e) {
            LOGGER.warning("buildObserverLocation error: " + e.getMessage());
            return npcLoc != null ? npcLoc.clone() : null;
        }
    }

    private void displayEvent(Player spectator, byte[] data) {
        if (data == null || data.length < 2) return;
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            byte magic = dis.readByte();
            ILangService lang = ServiceRegistry.get(ILangService.class);
            switch (magic) {
                case ReplayCodec.EVENT_DROP: {
                    String item = dis.readUTF();
                    int count = Math.max(dis.readInt(), 0);
                    spectator.sendMessage("§e[Replay] §7Dropped §f" + item + " §7x" + count);
                    break;
                }
                case ReplayCodec.EVENT_CHAT: {
                    String msg = dis.readUTF();
                    spectator.sendMessage("§e[Replay] §7Chat: §f" + msg);
                    break;
                }
                case ReplayCodec.EVENT_COMMAND: {
                    String cmd = dis.readUTF();
                    spectator.sendMessage("§e[Replay] §7Command: §f" + cmd);
                    break;
                }
                case ReplayCodec.EVENT_DAMAGE: {
                    String attacker = dis.readUTF();
                    double dmg = dis.readDouble();
                    String cause = dis.readUTF();
                    spectator.sendMessage("§e[Replay] §cDamage §7from §f" + attacker
                            + " §7(" + String.format("%.1f", dmg) + " §7" + cause + ")");
                    break;
                }
                case ReplayCodec.EVENT_DEATH: {
                    String deathMsg = dis.readUTF();
                    spectator.sendMessage("§e[Replay] §4Death: §f" + deathMsg);
                    break;
                }
                case ReplayCodec.EVENT_PROJECTILE: {
                    String projectile = dis.readUTF();
                    spectator.sendMessage("§e[Replay] §7Launched §f" + projectile);
                    break;
                }
                case ReplayCodec.EVENT_INTERACT: {
                    String target = dis.readUTF();
                    String type = dis.readUTF();
                    spectator.sendMessage("§e[Replay] §7Interacted with §f" + target + " §7(" + type + ")");
                    break;
                }
                case ReplayCodec.EVENT_PICKUP: {
                    String item = dis.readUTF();
                    int count = Math.max(dis.readInt(), 0);
                    spectator.sendMessage("§e[Replay] §7Picked up §f" + item + " §7x" + count);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warning("displayEvent error: " + e.getMessage());
        }
    }
}
