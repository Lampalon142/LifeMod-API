package fr.lampalon.lifemod.common.replay.storage;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.util.List;

/**
 * Defines the contract for writing recorded replay data.
 */
public interface ReplayWriter {

    /**
     * Initializes the storage for a specific replay session.
     * @param sessionName The unique name/ID of the replay.
     */
    void initialize(String sessionName);

    /**
     * Writes metadata header for the session.
     */
    void writeHeader(java.util.UUID playerUUID, int entityId, String playerName, String worldName, double x, double y, double z, float yaw, float pitch);

    /**
     * Writes a batch of recorded frames to the storage.
     * @param frames The frames to write.
     */
    void writeFrames(List<ReplayFrame> frames);

    /**
     * Closes the storage safely.
     */
    void close();
}
