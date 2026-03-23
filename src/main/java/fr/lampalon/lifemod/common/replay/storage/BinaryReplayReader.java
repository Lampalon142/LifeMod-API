package fr.lampalon.lifemod.common.replay.storage;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reader for binary replay files.
 */
public class BinaryReplayReader {

    private final File replayFile;

    private UUID playerUUID;
    private int entityId;
    private String playerName;
    private double startX, startY, startZ;
    private float startYaw, startPitch;

    public BinaryReplayReader(File replayFile) {
        this.replayFile = replayFile;
    }

    /**
     * Reads all frames from the replay file.
     * @return List of ReplayFrame objects.
     */
    public List<ReplayFrame> readAllFrames() {
        List<ReplayFrame> frames = new ArrayList<>();
        if (!replayFile.exists()) return frames;

        try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(replayFile)))) {
            if (inputStream.available() < 10) return frames;
            
            try {
                String magic = inputStream.readUTF();
                if ("LIFEREPLAY".equals(magic)) {
                    int version = inputStream.readInt();
                    this.playerUUID = new UUID(inputStream.readLong(), inputStream.readLong());
                    this.entityId = inputStream.readInt();
                    this.playerName = inputStream.readUTF();
                    this.startX = inputStream.readDouble();
                    this.startY = inputStream.readDouble();
                    this.startZ = inputStream.readDouble();
                    this.startYaw = inputStream.readFloat();
                    this.startPitch = inputStream.readFloat();
                } else {
                    return frames; 
                }
            } catch (EOFException e) {
                return frames;
            }

            while (inputStream.available() > 0) {
                try {
                    long timestamp = inputStream.readLong();
                    int packetCount = inputStream.readInt();
                    List<byte[]> packets = new ArrayList<>(packetCount);
                    for (int i = 0; i < packetCount; i++) {
                        int length = inputStream.readInt();
                        byte[] data = new byte[length];
                        inputStream.readFully(data);
                        packets.add(data);
                    }
                    frames.add(new ReplayFrame(timestamp, packets));
                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return frames;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public int getEntityId() { return entityId; }
    public String getPlayerName() { return playerName; }
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getStartZ() { return startZ; }
    public float getStartYaw() { return startYaw; }
    public float getStartPitch() { return startPitch; }
}
