package fr.lampalon.lifemod.common.replay.storage;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementation of ReplayWriter using binary format for storage.
 */
public class BinaryReplayWriter implements ReplayWriter {

    private static final Logger LOGGER = Logger.getLogger("BinaryReplayWriter");
    private DataOutputStream outputStream;
    private File sessionFile;

    @Override
    public void initialize(String sessionName) {
        File replayDir = new File("plugins/LifeMod/replays");
        if (!replayDir.exists()) {
            replayDir.mkdirs();
        }
        this.sessionFile = new File(replayDir, sessionName + ".replay");
        try {
            this.outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(sessionFile)));
            LOGGER.info("[DEBUG] Initialized writer for file: " + sessionFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.severe("[DEBUG] Failed to initialize writer for " + sessionName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void writeHeader(UUID playerUUID, int entityId, String playerName, double x, double y, double z, float yaw, float pitch) {
        if (outputStream == null) return;
        try {
            outputStream.writeUTF("LIFEREPLAY"); // Magic string
            outputStream.writeInt(1); // Version
            outputStream.writeLong(playerUUID.getMostSignificantBits());
            outputStream.writeLong(playerUUID.getLeastSignificantBits());
            outputStream.writeInt(entityId);
            outputStream.writeUTF(playerName);
            outputStream.writeDouble(x);
            outputStream.writeDouble(y);
            outputStream.writeDouble(z);
            outputStream.writeFloat(yaw);
            outputStream.writeFloat(pitch);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeFrames(List<ReplayFrame> frames) {
        if (outputStream == null) {
            LOGGER.warning("[DEBUG] writeFrames called but outputStream is null!");
            return;
        }
        try {
            int packetTotal = 0;
            for (ReplayFrame frame : frames) {
                outputStream.writeLong(frame.getTimestamp());
                outputStream.writeInt(frame.getPackets().size());
                for (byte[] packetData : frame.getPackets()) {
                    outputStream.writeInt(packetData.length);
                    outputStream.write(packetData);
                    packetTotal++;
                }
            }
            outputStream.flush();
            LOGGER.info("[DEBUG] Wrote " + frames.size() + " frames (" + packetTotal + " packets) to " + sessionFile.getName());
        } catch (IOException e) {
            LOGGER.severe("[DEBUG] Error writing frames to " + sessionFile.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (outputStream != null) {
            try {
                outputStream.close();
                LOGGER.info("[DEBUG] Closed writer for " + sessionFile.getName() + ". Final size: " + sessionFile.length() + " bytes.");
            } catch (IOException e) {
                LOGGER.severe("[DEBUG] Error closing writer for " + sessionFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
