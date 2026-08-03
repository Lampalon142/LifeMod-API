package fr.lampalon.lifemod.platform.bukkit.replay;

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Single source of truth for the replay wire format shared between recorders (writers)
 * and the playback engine (reader). Centralizes the magic bytes, the Minecraft VarInt
 * helpers and the position-frame parser so they are no longer duplicated/hard-coded
 * across listeners and the playback manager.
 */
public final class ReplayCodec {

    private ReplayCodec() {}

    // ── Frame type magic bytes ──────────────────────────────────────────────
    public static final byte POSITION = (byte) 0xFE;
    public static final byte ARM_SWING = (byte) 0xFB;
    public static final byte WORLD_PACKET = (byte) 0xFD;
    public static final byte ENTITY_PACKET = (byte) 0xFC;
    public static final byte SPAWN_ENTITY = (byte) 0xDD;
    public static final byte DESTROY_ENTITIES = (byte) 0xDC;
    public static final byte SPAWN_PLAYER = (byte) 0xDE;
    public static final byte SPAWN_MOB = (byte) 0xDB;
    public static final byte BLOCK_CHANGE = (byte) 0xF9;
    public static final byte BLOCK_UNDO = (byte) 0xFA;
    public static final byte EVENT_DROP = (byte) 0xF1;
    public static final byte EVENT_CHAT = (byte) 0xF2;
    public static final byte EVENT_COMMAND = (byte) 0xF3;
    public static final byte EVENT_DAMAGE = (byte) 0xF4;
    public static final byte EVENT_DEATH = (byte) 0xF5;
    public static final byte EVENT_PROJECTILE = (byte) 0xF6;
    public static final byte EVENT_INTERACT = (byte) 0xF7;
    public static final byte EVENT_PICKUP = (byte) 0xF8;

    // ── Minecraft VarInt (on a Netty ByteBuf) ──────────────────────────────
    public static int readVarInt(ByteBuf buf) {
        int v = 0, s = 0;
        byte b;
        do {
            b = buf.readByte();
            v |= (b & 0x7F) << s;
            s += 7;
        } while ((b & 0x80) != 0);
        return v;
    }

    public static void writeVarInt(ByteBuf buf, int v) {
        while ((v & ~0x7F) != 0) {
            buf.writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        buf.writeByte(v);
    }

    public static int varIntSize(int v) {
        int s = 0;
        do {
            v >>>= 7;
            s++;
        } while (v != 0);
        return s;
    }

    // ── Position frame (0xFE): [magic][double x,y,z][float yaw,pitch][boolean onGround] ──
    public static final class Position {
        public final double x, y, z;
        public final float yaw, pitch;
        public final boolean onGround;

        public Position(double x, double y, double z, float yaw, float pitch, boolean onGround) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
        }
    }

    public static Position parsePosition(byte[] data) {
        if (data == null || data.length < 34) return null;
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            dis.readByte(); // magic
            return new Position(dis.readDouble(), dis.readDouble(), dis.readDouble(),
                    dis.readFloat(), dis.readFloat(), dis.readBoolean());
        } catch (IOException e) {
            return null;
        }
    }

    // ── Spawn-player frame (0xDE): [magic][int eId][long uuidMost][long uuidLeast]
    //    [UTF name][double x,y,z][float yaw,pitch] ──
    public static byte[] buildSpawnPlayerFrame(int entityId, UUID uuid, String name,
                                               double x, double y, double z, float yaw, float pitch) {
        if (uuid == null || name == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeByte(SPAWN_PLAYER);
            dos.writeInt(entityId);
            dos.writeLong(uuid.getMostSignificantBits());
            dos.writeLong(uuid.getLeastSignificantBits());
            dos.writeUTF(name);
            dos.writeDouble(x); dos.writeDouble(y); dos.writeDouble(z);
            dos.writeFloat(yaw); dos.writeFloat(pitch);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    // ── Spawn-mob frame (0xDB): [magic][int eId][UTF typeName]
    //    [double x,y,z][float yaw,pitch] ──
    public static byte[] buildSpawnMobFrame(int entityId, String typeName,
                                            double x, double y, double z, float yaw, float pitch) {
        if (typeName == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeByte(SPAWN_MOB);
            dos.writeInt(entityId);
            dos.writeUTF(typeName);
            dos.writeDouble(x); dos.writeDouble(y); dos.writeDouble(z);
            dos.writeFloat(yaw); dos.writeFloat(pitch);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    // ── Destroy frame (0xDC): [magic][VarInt count=1][VarInt entityId] ──
    public static byte[] buildDestroyFrame(int entityId) {
        ByteBuf buf = io.netty.buffer.Unpooled.buffer();
        try {
            buf.writeByte(DESTROY_ENTITIES);
            writeVarInt(buf, 1);
            writeVarInt(buf, entityId);
            byte[] result = new byte[buf.readableBytes()];
            buf.readBytes(result);
            return result;
        } finally {
            buf.release();
        }
    }
}
