package fr.lampalon.lifemod.api.scan;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;

public class ScanResult {

    private final Map<Location, Integer> foundLocations;
    private final Map<UUID, Integer> foundPlayers;
    private final int totalCount;
    private final long durationMs;

    public ScanResult(Map<Location, Integer> foundLocations, Map<UUID, Integer> foundPlayers, int totalCount, long durationMs) {
        this.foundLocations = foundLocations;
        this.foundPlayers = foundPlayers;
        this.totalCount = totalCount;
        this.durationMs = durationMs;
    }

    public Map<Location, Integer> getFoundLocations() {
        return foundLocations;
    }

    public Map<UUID, Integer> getFoundPlayers() {
        return foundPlayers;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
