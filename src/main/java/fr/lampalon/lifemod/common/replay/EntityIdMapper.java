package fr.lampalon.lifemod.common.replay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles mapping of real entity IDs to unique virtual IDs for replay playback.
 * Prevents conflicts with actual server entities.
 */
public class EntityIdMapper {

    private final Map<Integer, Integer> idMapping = new ConcurrentHashMap<>();
    private final AtomicInteger nextVirtualId = new AtomicInteger(1000000); // Start high to avoid conflicts

    /**
     * Maps a real entity ID to a virtual one.
     * @param realId The ID captured during recording.
     * @return The unique virtual ID for playback.
     */
    public int getVirtualId(int realId) {
        return idMapping.computeIfAbsent(realId, k -> nextVirtualId.getAndIncrement());
    }

    /**
     * Clears all mappings (e.g., when ending playback).
     */
    public void clear() {
        idMapping.clear();
    }
}
