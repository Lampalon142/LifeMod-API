package fr.lampalon.lifemod.platform.bukkit.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import fr.lampalon.lifemod.common.nms.api.NMSReplayHandler;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayPacketListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Magic byte routing:
 *   0xFE = position      → EntityTeleport + HeadLook
 *   0xFD = world packet  → send raw
 *   0xFC = entity packet → rewrite entity ID
 *   0xFB = arm swing     → EntityAnimation
 *   0xFA = undo block    → used only at init restore, skipped during playback
 *   0xF9 = block change  → WrapperPlayServerBlockChange (placed/broken block)
 */
public class PlaybackManager {
    private static final Logger LOGGER = Logger.getLogger("PlaybackManager");
    private final AtomicInteger virtualIdCounter = new AtomicInteger(900_000);

    private final LifeMod plugin;
    private List<ReplayFrame> frames;
    private int currentIndex = 0;
    private int virtualEntityId;
    private UUID npcUUID;
    private BukkitTask playbackTask;
    private boolean isPaused = false;
    private double playbackSpeed = 1.0;

    public PlaybackManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Start
    // -------------------------------------------------------------------------

    public void startPlayback(Player spectator, List<ReplayFrame> frames, ReplaySession session) {
        startPlayback(spectator, frames,
                session.getEntityId(), session.getPlayerUUID(), session.getPlayerName(),
                new Location(spectator.getWorld(),
                        session.getStartX(), session.getStartY(), session.getStartZ(),
                        session.getStartYaw(), session.getStartPitch()));
    }

    public void startPlayback(Player spectator, List<ReplayFrame> frames,
                              int originalEntityId, UUID targetUUID,
                              String targetName, Location startLoc) {
        this.frames = frames;
        this.currentIndex = 0;
        this.isPaused = false;
        this.playbackSpeed = 1.0;
        this.npcUUID = UUID.randomUUID();
        this.virtualEntityId = virtualIdCounter.getAndIncrement();

        LOGGER.info("[DEBUG] Starting playback for " + spectator.getName()
                + " | Target: " + targetName
                + " | originalId: " + originalEntityId
                + " | virtualId: " + virtualEntityId
                + " | moderatorId: " + spectator.getEntityId());

        plugin.getReplayPlayerManager().registerPlayback(spectator, this);

        Player realPlayer = Bukkit.getPlayer(targetUUID);
        if (realPlayer != null && realPlayer.isOnline()) spectator.hidePlayer(plugin, realPlayer);

        spectator.teleport(buildObserverLocation(startLoc));

        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getReplayManager().getSkinManager().getOrFetchSkin(targetUUID);
            }
        }.runTaskAsynchronously(plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!spectator.isOnline()) return;

                TextureProperty[] skin = plugin.getReplayManager()
                        .getSkinManager().getSkinOrByName(targetUUID, targetName);

                LOGGER.info("[DEBUG] Spawning NPC | skin: "
                        + (skin == null || skin.length == 0 ? "NULL" : "OK"));

                NMSReplayHandler nms = (NMSReplayHandler)
                        ServiceRegistry.get(ILifePlatform.class).getNmsProvider();
                nms.spawnNPC(spectator, virtualEntityId, npcUUID, targetName, skin, startLoc);

                restoreBlocks(spectator);
                startPlaybackLoop(spectator, realPlayer, originalEntityId);
            }
        }.runTaskLater(plugin, 20L);
    }

    // -------------------------------------------------------------------------
    // Block restore at replay start
    // -------------------------------------------------------------------------

    /**
     * Sends 0xFA undo packets to the moderator's client to visually restore
     * the world to its state at the START of the recording.
     * Deduplication keeps only the EARLIEST (= original) state per position.
     */
    private void restoreBlocks(Player spectator) {
        Map<Long, Integer> toRestore = new HashMap<>();

        for (int i = frames.size() - 1; i >= 0; i--) {
            for (byte[] data : frames.get(i).getPackets()) {
                if (data.length < 14 || data[0] != (byte) 0xFA) continue;
                try {
                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
                    dis.readByte();
                    int x = dis.readInt(), y = dis.readInt(), z = dis.readInt();
                    int blockId = dis.readInt();
                    toRestore.put(encodePos(x, y, z), blockId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        int count = 0;
        for (Map.Entry<Long, Integer> entry : toRestore.entrySet()) {
            int x = decodeX(entry.getKey());
            int y = decodeY(entry.getKey());
            int z = decodeZ(entry.getKey());
            try {
                PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                        new WrapperPlayServerBlockChange(
                                new com.github.retrooper.packetevents.util.Vector3i(x, y, z),
                                entry.getValue()));
                count++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LOGGER.info("[DEBUG] Block restore: " + count + " blocks for " + spectator.getName());
        fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        spectator.sendMessage(lang.getMessage("replay.playback.blocks-restored", "%count%", String.valueOf(count)));
    }

    // -------------------------------------------------------------------------
    // Stop / Pause / Speed / Seek
    // -------------------------------------------------------------------------

    public void stopPlayback() {
        if (playbackTask != null && !playbackTask.isCancelled()) {
            playbackTask.cancel();
            playbackTask = null;
        }
    }

    public void setPaused(boolean paused) { this.isPaused = paused; }
    public boolean isPaused() { return isPaused; }
    public void setPlaybackSpeed(double speed) { this.playbackSpeed = speed; }
    public int getCurrentIndex() { return currentIndex; }

    public void seekTo(Player spectator, int targetIndex) {
        if (targetIndex < 0) targetIndex = 0;
        if (targetIndex >= frames.size()) targetIndex = frames.size() - 1;

            if (targetIndex < currentIndex) {
                Map<Long, Integer> toRestore = new HashMap<>();
                for (int i = currentIndex - 1; i >= targetIndex; i--) {
                    for (byte[] data : frames.get(i).getPackets()) {
                        if (data.length < 14 || data[0] != (byte) 0xFA) continue;
                        try {
                            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
                            dis.readByte();
                            int x = dis.readInt(), y = dis.readInt(), z = dis.readInt(), blockId = dis.readInt();
                            toRestore.put(encodePos(x, y, z), blockId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                for (Map.Entry<Long, Integer> entry : toRestore.entrySet()) {
                    try {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                                new WrapperPlayServerBlockChange(
                                        new com.github.retrooper.packetevents.util.Vector3i(
                                                decodeX(entry.getKey()), decodeY(entry.getKey()), decodeZ(entry.getKey())),
                                        entry.getValue()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                for (int i = currentIndex; i < targetIndex; i++) {
                    for (byte[] data : frames.get(i).getPackets()) {
                        if (data.length < 1) continue;
                        try { dispatchPacket(spectator, data, -1); } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        this.currentIndex = targetIndex;
    }

    // -------------------------------------------------------------------------
    // Playback loop
    // -------------------------------------------------------------------------

    private void startPlaybackLoop(Player spectator, Player realPlayer, int originalEntityId) {
        playbackTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!spectator.isOnline()
                        || !plugin.getReplayPlayerManager().isInReplay(spectator)) {
                    cleanup(spectator, realPlayer);
                    this.cancel();
                    return;
                }

                if (isPaused) return;

                if (currentIndex >= frames.size()) {
                    LOGGER.info("[DEBUG] Replay finished for " + spectator.getName());
                    cleanup(spectator, realPlayer);
                    plugin.getReplayPlayerManager().exitReplay(spectator);
                    fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                    spectator.sendMessage(lang.getMessage("replay.playback.finished"));
                    this.cancel();
                    return;
                }

                for (byte[] data : frames.get(currentIndex).getPackets()) {
                    if (data.length == 0) continue;
                    try { dispatchPacket(spectator, data, originalEntityId); }
                    catch (Exception e) { e.printStackTrace(); }
                }

                currentIndex++;

                if (currentIndex % 200 == 0) {
                    fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                    spectator.sendMessage(lang.getMessage("replay.playback.progress",
                            "%percent%", String.valueOf((int)((currentIndex / (double) frames.size()) * 100))));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // -------------------------------------------------------------------------
    // Packet dispatching
    // -------------------------------------------------------------------------

    private void dispatchPacket(Player spectator, byte[] data, int originalEntityId)
            throws Exception {
        byte magic = data[0];

        // ── 0xFE : position ───────────────────────────────────────────────────
        if (magic == (byte) 0xFE) {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            dis.readByte();
            double x = dis.readDouble(), y = dis.readDouble(), z = dis.readDouble();
            float yaw = dis.readFloat(), pitch = dis.readFloat();
            boolean onGround = dis.readBoolean();
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                    new WrapperPlayServerEntityTeleport(virtualEntityId,
                            new Vector3d(x, y, z), yaw, pitch, onGround));
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                    new WrapperPlayServerEntityHeadLook(virtualEntityId, yaw));
            return;
        }

        // ── 0xFB : arm swing ─────────────────────────────────────────────────
        if (magic == (byte) 0xFB) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                    new WrapperPlayServerEntityAnimation(virtualEntityId,
                            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM));
            return;
        }

        // ── 0xFA : undo — skip during playback, only used at init ─────────────
        if (magic == (byte) 0xFA) return;

        // ── 0xF9 : block change (place/break recorded by ReplayBlockListener) ──
        if (magic == (byte) 0xF9) {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            dis.readByte(); // skip 0xF9
            int x = dis.readInt(), y = dis.readInt(), z = dis.readInt();
            int blockId = dis.readInt();
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator,
                    new WrapperPlayServerBlockChange(
                            new com.github.retrooper.packetevents.util.Vector3i(x, y, z),
                            blockId));
            return;
        }

        // Strip magic byte
        byte[] raw = new byte[data.length - 1];
        System.arraycopy(data, 1, raw, 0, raw.length);

        // ── 0xFD : world packet — send as-is ─────────────────────────────────
        if (magic == ReplayPacketListener.MAGIC_WORLD_PACKET) {
            PacketEvents.getAPI().getProtocolManager().sendPacket(
                    spectator, Unpooled.wrappedBuffer(raw));
            return;
        }

        // ── 0xFC : entity packet — rewrite entity ID ──────────────────────────
        if (magic == ReplayPacketListener.MAGIC_ENTITY_PACKET) {
            if (originalEntityId >= 0) {
                byte[] rewritten = rewriteEntityId(raw, originalEntityId, virtualEntityId);
                PacketEvents.getAPI().getProtocolManager().sendPacket(
                        spectator, Unpooled.wrappedBuffer(rewritten));
            }
            return;
        }

        // Legacy — send as-is
        PacketEvents.getAPI().getProtocolManager().sendPacket(
                spectator, Unpooled.wrappedBuffer(data));
    }

    // -------------------------------------------------------------------------
    // Entity ID rewriting
    // -------------------------------------------------------------------------

    private byte[] rewriteEntityId(byte[] data, int originalId, int virtualId) {
        try {
            ByteBuf buf = Unpooled.wrappedBuffer(data);
            readVarInt(buf);
            int startIdx = buf.readerIndex();
            int entityId = readVarInt(buf);
            int endIdx = buf.readerIndex();
            if (entityId != originalId) return data;

            ByteBuf out = Unpooled.buffer(data.length - varIntSize(originalId) + varIntSize(virtualId));
            buf.readerIndex(0);
            buf.readBytes(out, startIdx);
            writeVarInt(out, virtualId);
            buf.readerIndex(endIdx);
            buf.readBytes(out, buf.readableBytes());
            byte[] result = new byte[out.readableBytes()];
            out.readBytes(result);
            return result;
        } catch (Exception e) { return data; }
    }

    private int readVarInt(ByteBuf buf) {
        int v = 0, s = 0; byte b;
        do { b = buf.readByte(); v |= (b & 0x7F) << s; s += 7; } while ((b & 0x80) != 0);
        return v;
    }

    private void writeVarInt(ByteBuf buf, int v) {
        while ((v & ~0x7F) != 0) { buf.writeByte((v & 0x7F) | 0x80); v >>>= 7; }
        buf.writeByte(v);
    }

    private int varIntSize(int v) {
        int s = 0; do { v >>>= 7; s++; } while (v != 0); return s;
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

    private void cleanup(Player spectator, Player realPlayer) {
        NMSReplayHandler nms = (NMSReplayHandler)
                ServiceRegistry.get(ILifePlatform.class).getNmsProvider();
        nms.removeNPC(spectator, virtualEntityId, npcUUID);
        if (realPlayer != null && realPlayer.isOnline()) spectator.showPlayer(plugin, realPlayer);
    }

    private Location buildObserverLocation(Location npcLoc) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        double distance = config.getDouble("modules.replay.observer-distance", 5.0);
        double yOffset = config.getDouble("modules.replay.observer-y-offset", 2.0);
        double yawOffset = config.getDouble("modules.replay.observer-yaw-offset", 180.0);
        float pitch = (float) config.getDouble("modules.replay.observer-pitch", 15.0);

        double yawRad = Math.toRadians(npcLoc.getYaw());
        Location obs = npcLoc.clone();
        obs.setX(npcLoc.getX() + Math.sin(yawRad) * distance);
        obs.setY(npcLoc.getY() + yOffset);
        obs.setZ(npcLoc.getZ() - Math.cos(yawRad) * distance);
        obs.setYaw(npcLoc.getYaw() + (float) yawOffset);
        obs.setPitch(pitch);
        return obs;
    }
}