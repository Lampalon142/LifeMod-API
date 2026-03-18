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
     * Writes a batch of recorded frames to the storage.
     * @param frames The frames to write.
     */
    void writeFrames(List<ReplayFrame> frames);

    /**
     * Closes the storage safely.
     */
    void close();
}
