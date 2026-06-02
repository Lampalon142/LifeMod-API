package fr.lampalon.lifemod.common.replay.buffer;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A circular-like buffer that keeps the last 1 hour of replay data in memory.
 * This is designed for high-performance recording with quick retrieval.
 */
public class ReplayBuffer {

    private static final long MAX_DURATION_MS = 3600000; // 1 hour in milliseconds
    private final ConcurrentLinkedDeque<ReplayFrame> frames = new ConcurrentLinkedDeque<>();

    /**
     * Appends a frame to the buffer and removes frames older than 1 hour.
     * @param frame The frame to record.
     */
    public void addFrame(ReplayFrame frame) {
        frames.addLast(frame);
        cleanup();
    }

    /**
     * Removes frames that are older than the maximum allowed duration (1 hour).
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        while (!frames.isEmpty() && (now - frames.peekFirst().getTimestamp() > MAX_DURATION_MS)) {
            frames.pollFirst();
        }
    }

    /**
     * Retrieves the last frames for a specific duration.
     * @param durationMs The duration in milliseconds to retrieve.
     * @return A list of frames within the requested duration.
     */
    public List<ReplayFrame> getFrames(long durationMs) {
        long now = System.currentTimeMillis();
        long startTime = now - durationMs;
        
        List<ReplayFrame> result = new ArrayList<>();
        // Iterate backwards from the most recent frames
        var iterator = frames.descendingIterator();
        while (iterator.hasNext()) {
            ReplayFrame frame = iterator.next();
            if (frame.getTimestamp() >= startTime) {
                result.add(frame);
            } else {
                // Since frames are chronological, we can stop here
                break;
            }
        }
        
        // Reverse because we collected them in descending order
        Collections.reverse(result);
        return result;
    }

    /**
     * Clears all frames from the buffer.
     */
    public void clear() {
        frames.clear();
    }

    /**
     * Returns the current size of the buffer.
     * @return Current buffer size.
     */
    public int size() {
        return frames.size();
    }
}
