package fr.lampalon.lifemod.common.replay.storage;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Writes replays to a compressed binary file (fallback when no database is configured).
 * The full payload is buffered and flushed/compressed once on {@link #close()}, which is
 * how the recording lifecycle invokes it (a single {@code writeFrames} at session stop).
 */
public class BinaryReplayWriter implements ReplayWriter {

    private static final Logger LOGGER = Logger.getLogger("BinaryReplayWriter");

    private File sessionFile;
    private UUID playerUUID;
    private int entityId;
    private String playerName;
    private double startX, startY, startZ;
    private float startYaw, startPitch;
    private boolean headerWritten;
    private final List<ReplayFrame> pending = new ArrayList<>();

    @Override
    public void initialize(String sessionName) {
        File replayDir = new File("plugins/LifeMod/replays");
        if (!replayDir.exists()) replayDir.mkdirs();
        this.sessionFile = new File(replayDir, sessionName + ".replay");
    }

    @Override
    public void writeHeader(UUID playerUUID, int entityId, String playerName, String worldName,
                            double x, double y, double z, float yaw, float pitch) {
        this.playerUUID = playerUUID;
        this.entityId = entityId;
        this.playerName = playerName;
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.startYaw = yaw;
        this.startPitch = pitch;
        this.headerWritten = true;
    }

    @Override
    public void writeFrames(List<ReplayFrame> frames) {
        if (frames != null) pending.addAll(frames);
    }

    @Override
    public void close() {
        if (sessionFile == null || playerUUID == null || !headerWritten) return;
        try {
            ReplayFormat.SerializedReplay serialized = ReplayFormat.serialize(
                    playerUUID, entityId, playerName, startX, startY, startZ, startYaw, startPitch, pending);
            byte[] envelope = ReplayFormat.toEnvelope(serialized.payload);
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(sessionFile)))) {
                out.write(envelope);
            }
            LOGGER.fine("Wrote " + serialized.frameCount + " frames to " + sessionFile.getName()
                    + " (" + envelope.length + " bytes, compressed)");
        } catch (IOException e) {
            LOGGER.severe("Failed to write " + sessionFile.getName() + ": " + e.getMessage());
        }
    }
}
