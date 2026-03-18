package fr.lampalon.lifemod.common.replay;

import fr.lampalon.lifemod.common.replay.buffer.ReplayBuffer;
import fr.lampalon.lifemod.common.replay.storage.BinaryReplayWriter;
import fr.lampalon.lifemod.common.replay.storage.ReplayWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the replay recording lifecycle, including buffering and disk flushing.
 */
public class ReplayManager {

    private final ReplayBuffer buffer;
    private final ReplayWriter writer;
    private final ScheduledExecutorService scheduler;
    private boolean recording;

    public ReplayManager() {
        this.buffer = new ReplayBuffer();
        this.writer = new BinaryReplayWriter();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts recording a session.
     * @param sessionName Unique session name.
     */
    public void startRecording(String sessionName) {
        this.writer.initialize(sessionName);
        this.recording = true;

        // Schedule periodic flush (every 1 second to keep memory usage low)
        scheduler.scheduleAtFixedRate(() -> {
            if (!buffer.flush().isEmpty()) {
                // Here we would implement the segmentation logic (5 mins max per file)
                writer.writeFrames(buffer.flush());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Stops the recording process.
     */
    public void stopRecording() {
        this.recording = false;
        scheduler.shutdown();
        writer.close();
    }

    public ReplayBuffer getBuffer() {
        return buffer;
    }
}
