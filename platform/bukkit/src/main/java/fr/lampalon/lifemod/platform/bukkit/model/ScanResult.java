package fr.lampalon.lifemod.platform.bukkit.model;

import org.bukkit.Location;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanResult {
    private final Map<Location, Integer> foundLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> foundPlayers = new ConcurrentHashMap<>();
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private final long startTime;
    private long endTime;

    public ScanResult() {
        this.startTime = System.currentTimeMillis();
    }

    public void addLocation(Location location, int count) {
        Location blockLoc = location.getBlock().getLocation();
        foundLocations.merge(blockLoc, count, Integer::sum);
        totalCount.addAndGet(count);
    }

    public void addPlayer(UUID playerUUID, int count) {
        foundPlayers.merge(playerUUID, count, Integer::sum);
        totalCount.addAndGet(count);
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
        return totalCount.get();
    }

    public long getDuration() {
        return endTime - startTime;
    }
}
