package fr.lampalon.lifemod.common.replay.buffer;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe buffer for incoming replay frames to maintain performance.
 */
public class ReplayBuffer {

    private final ConcurrentLinkedQueue<ReplayFrame> frameQueue = new ConcurrentLinkedQueue<>();

    /**
     * Appends a frame to the queue.
     * @param frame The frame to record.
     */
    public void addFrame(ReplayFrame frame) {
        frameQueue.add(frame);
    }

    /**
     * Drains the queue for asynchronous processing.
     * @return List of current recorded frames.
     */
    public List<ReplayFrame> flush() {
        List<ReplayFrame> flushedFrames = new ArrayList<>();
        ReplayFrame frame;
        while ((frame = frameQueue.poll()) != null) {
            flushedFrames.add(frame);
        }
        return flushedFrames;
    }

    /**
     * Returns the current size of the buffer.
     * @return Current buffer size.
     */
    public int size() {
        return frameQueue.size();
    }
}
