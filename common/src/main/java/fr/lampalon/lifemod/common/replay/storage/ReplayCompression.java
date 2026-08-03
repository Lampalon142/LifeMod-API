package fr.lampalon.lifemod.common.replay.storage;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * zlib compression for serialized replay payloads.
 *
 * Replay payloads are streams of Minecraft packets with a lot of repetitive
 * structure, so compression typically shrinks them by one or two orders of
 * magnitude. This directly addresses database/file bloat and drastically cuts
 * the amount of data written/read on the main thread.
 */
public final class ReplayCompression {

    private ReplayCompression() {}

    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) return data;
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(data.length / 2, 32));
        byte[] buf = new byte[8192];
        while (!deflater.finished()) {
            int n = deflater.deflate(buf);
            out.write(buf, 0, n);
        }
        deflater.end();
        return out.toByteArray();
    }

    public static byte[] decompress(byte[] data, int originalEstimate) {
        if (data == null || data.length == 0) return data;
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream(
                originalEstimate > 0 ? originalEstimate : data.length * 4);
        byte[] buf = new byte[8192];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n == 0 && inflater.needsInput()) break;
                out.write(buf, 0, n);
            }
        } catch (java.util.zip.DataFormatException e) {
            throw new RuntimeException("Corrupted replay data", e);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }
}
