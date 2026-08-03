package fr.lampalon.lifemod.common.replay.buffer;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A circular-like buffer that keeps the last 1 hour of replay data in memory.
 * This is designed for high-performance recording with quick retrieval.
 */
public class ReplayBuffer {

    private static final long MAX_DURATION_MS = 3600000; // 1 hour in milliseconds
    private static final int MAX_FRAMES = 500_000; // hard memory ceiling regardless of time
    private final ConcurrentLinkedDeque<ReplayFrame> frames = new ConcurrentLinkedDeque<>();
    private final AtomicInteger count = new AtomicInteger();

    /**
     * Appends a frame to the buffer and removes frames older than 1 hour.
     * @param frame The frame to record.
     */
    public void addFrame(ReplayFrame frame) {
        frames.addLast(frame);
        count.incrementAndGet();
        cleanup();
    }

    /**
     * Removes frames that are older than the maximum allowed duration (1 hour).
     * Uses an O(1) counter instead of {@code ConcurrentLinkedDeque.size()}, which is O(n)
     * and would otherwise be called on every recorded frame (the hot path).
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        while (!frames.isEmpty() && (now - frames.peekFirst().getTimestamp() > MAX_DURATION_MS)) {
            frames.pollFirst();
            count.decrementAndGet();
        }
        while (count.get() > MAX_FRAMES && !frames.isEmpty()) {
            frames.pollFirst();
            count.decrementAndGet();
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
        count.set(0);
    }

    /**
     * Returns the current size of the buffer.
     * @return Current buffer size.
     */
    public int size() {
        return count.get();
    }
}
