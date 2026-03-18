package fr.lampalon.lifemod.common.replay.storage;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader for binary replay files.
 */
public class BinaryReplayReader {

    private final File replayFile;

    public BinaryReplayReader(File replayFile) {
        this.replayFile = replayFile;
    }

    /**
     * Reads all frames from the replay file.
     * @return List of ReplayFrame objects.
     */
    public List<ReplayFrame> readAllFrames() {
        List<ReplayFrame> frames = new ArrayList<>();
        try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(replayFile)))) {
            while (inputStream.available() > 0) {
                long timestamp = inputStream.readLong();
                int packetCount = inputStream.readInt();
                List<Object> packets = new ArrayList<>();
                for (int i = 0; i < packetCount; i++) {
                    int length = inputStream.readInt();
                    byte[] packetBytes = new byte[length];
                    inputStream.readFully(packetBytes);
                    com.github.retrooper.packetevents.netty.buffer.ByteBufHelper helper = com.github.retrooper.packetevents.PacketEvents.getAPI().getNettyManager().getByteBufHelper();
                    com.github.retrooper.packetevents.netty.buffer.ByteBuf buffer = helper.allocate(length);
                    buffer.writeBytes(packetBytes);
                    // Reconstruct packet from the buffer using PacketEvents protocol manager
                    // packets.add(com.github.retrooper.packetevents.PacketEvents.getAPI().getProtocolManager().readPacket(buffer));
                    buffer.release();
                }
                frames.add(new ReplayFrame(timestamp, packets));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return frames;
    }
}
