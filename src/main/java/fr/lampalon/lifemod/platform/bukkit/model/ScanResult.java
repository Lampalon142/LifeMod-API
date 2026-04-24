package fr.lampalon.lifemod.platform.bukkit.model;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScanResult {
    private final Map<Location, Integer> foundLocations = new HashMap<>();
    private final Map<UUID, Integer> foundPlayers = new HashMap<>();
    private int totalCount = 0;
    private final long startTime;
    private long endTime;

    public ScanResult() {
        this.startTime = System.currentTimeMillis();
    }

    public void addLocation(Location location, int count) {
        foundLocations.put(location, foundLocations.getOrDefault(location, 0) + count);
        totalCount += count;
    }

    public void addPlayer(UUID playerUUID, int count) {
        foundPlayers.put(playerUUID, foundPlayers.getOrDefault(playerUUID, 0) + count);
        totalCount += count;
    }

    public void complete() {
        this.endTime = System.currentTimeMillis();
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

    public long getDuration() {
        return endTime - startTime;
    }
}
