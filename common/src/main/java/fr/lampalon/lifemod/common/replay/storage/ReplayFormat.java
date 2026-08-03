package fr.lampalon.lifemod.common.replay.storage;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Single source of truth for the on-disk / in-database replay binary format.
 *
 * Layout (version 2, compressed):
 *   [UTF "LIFEREPLAY"][int version=2][int uncompressedSize][compressed payload]
 *
 * The payload (identical for version 1, but uncompressed) is:
 *   [long uuidMost][long uuidLeast][int entityId][UTF playerName]
 *   [double x][double y][double z][float yaw][float pitch]
 *   then, per frame: [long timestamp][int packetCount][per packet: int length][bytes]
 *
 * Version 1 (legacy, uncompressed) is still readable for backward compatibility.
 */
public final class ReplayFormat {

    public static final int VERSION = 2;
    public static final String MAGIC = "LIFEREPLAY";

    private ReplayFormat() {}

    public static final class SerializedReplay {
        public final byte[] payload;
        public final int frameCount;
        public final long firstTs;
        public final long lastTs;

        SerializedReplay(byte[] payload, int frameCount, long firstTs, long lastTs) {
            this.payload = payload;
            this.frameCount = frameCount;
            this.firstTs = firstTs;
            this.lastTs = lastTs;
        }
    }

    public static final class ParsedReplay {
        public UUID playerUUID;
        public int entityId;
        public String playerName;
        public double startX, startY, startZ;
        public float startYaw, startPitch;
        public List<ReplayFrame> frames = new ArrayList<>();
    }

    public static SerializedReplay serialize(UUID playerUUID, int entityId, String playerName,
                                             double x, double y, double z, float yaw, float pitch,
                                             List<ReplayFrame> frames) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int frameCount = 0;
        long firstTs = 0, lastTs = 0;
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeLong(playerUUID.getMostSignificantBits());
            out.writeLong(playerUUID.getLeastSignificantBits());
            out.writeInt(entityId);
            out.writeUTF(playerName != null ? playerName : "unknown");
            out.writeDouble(x);
            out.writeDouble(y);
            out.writeDouble(z);
            out.writeFloat(yaw);
            out.writeFloat(pitch);
            if (frames != null) {
                for (ReplayFrame frame : frames) {
                    if (frame == null) continue;
                    List<byte[]> packets = frame.getPackets();
                    if (packets == null || packets.isEmpty()) continue;
                    out.writeLong(frame.getTimestamp());
                    out.writeInt(packets.size());
                    for (byte[] p : packets) {
                        if (p == null || p.length == 0) continue;
                        out.writeInt(p.length);
                        out.write(p);
                    }
                    frameCount++;
                    long ts = frame.getTimestamp();
                    if (firstTs == 0) firstTs = ts;
                    lastTs = ts;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize replay", e);
        }
        return new SerializedReplay(baos.toByteArray(), frameCount, firstTs, lastTs);
    }

    /**
     * Builds the full on-disk envelope around a payload: magic + version + uncompressed size + zlib-compressed payload.
     */
    public static byte[] toEnvelope(byte[] payload) {
        byte[] compressed = ReplayCompression.compress(payload);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(compressed.length + 16);
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeUTF(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(payload.length);
            out.write(compressed);
        } catch (IOException e) {
            throw new RuntimeException("Failed to build replay envelope", e);
        }
        return baos.toByteArray();
    }

    public static ParsedReplay parse(byte[] raw, int maxFrames, int maxPacketLen) {
        if (raw == null || raw.length < 12) return null;
        ParsedReplay result = new ParsedReplay();
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw));
            String magic = in.readUTF();
            if (!MAGIC.equals(magic)) return null;
            int version = in.readInt();
            if (version == 2) {
                int uncompressedSize = in.readInt();
                byte[] compressed = in.readAllBytes();
                byte[] payload = ReplayCompression.decompress(compressed, uncompressedSize);
                in = new DataInputStream(new ByteArrayInputStream(payload));
            } else if (version != 1) {
                return null;
            }

            result.playerUUID = new UUID(in.readLong(), in.readLong());
            result.entityId = in.readInt();
            result.playerName = in.readUTF();
            result.startX = in.readDouble();
            result.startY = in.readDouble();
            result.startZ = in.readDouble();
            result.startYaw = in.readFloat();
            result.startPitch = in.readFloat();

            int frameCount = 0;
            while (frameCount < maxFrames) {
                try {
                    long ts = in.readLong();
                    int packetCount = in.readInt();
                    if (packetCount <= 0 || packetCount > 10000) break;
                    List<byte[]> packets = new ArrayList<>(packetCount);
                    for (int i = 0; i < packetCount; i++) {
                        int len = in.readInt();
                        if (len <= 0 || len > maxPacketLen) break;
                        byte[] data = new byte[len];
                        in.readFully(data);
                        packets.add(data);
                    }
                    if (packets.size() != packetCount) break;
                    result.frames.add(new ReplayFrame(ts, packets));
                    frameCount++;
                } catch (EOFException e) {
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
