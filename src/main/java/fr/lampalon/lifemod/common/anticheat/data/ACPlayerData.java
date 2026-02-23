package fr.lampalon.lifemod.common.anticheat.data;

import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Stores all real-time data, network samples, and statistical counters for a specific player.
 * Data-Driven approach: Sliding windows (Samples) for combat and movement.
 */
public final class ACPlayerData {

    private final UUID uuid;
    
    // Network data
    private long ping;
    private String clientBrand;
    private long lastTransactionTime;

    // Combat Samples (Sliding windows)
    private final FixedSizeQueue<Long> clickIntervals = new FixedSizeQueue<>(100);
    private final Map<Integer, EntityState> entityStates = new ConcurrentHashMap<>();

    // Rotation Samples
    private final FixedSizeQueue<Float> yawDeltas = new FixedSizeQueue<>(50);
    private final FixedSizeQueue<Float> pitchDeltas = new FixedSizeQueue<>(50);

    // Timer Balance
    private long timerBalance = 0;
    private long lastMoveTime = -1;
    private long lastSwingTime = -1;

    // Probabilities & Confidence Score
    private double confidenceScore = 0.0;
    private final Map<String, Integer> violationLevels = new ConcurrentHashMap<>();

    public ACPlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public long getTimerBalance() {
        return timerBalance;
    }

    public void setTimerBalance(long timerBalance) {
        this.timerBalance = timerBalance;
    }

    public long getLastMoveTime() {
        return lastMoveTime;
    }

    public void setLastMoveTime(long lastMoveTime) {
        this.lastMoveTime = lastMoveTime;
    }

    public long getLastSwingTime() {
        return lastSwingTime;
    }

    public void setLastSwingTime(long lastSwingTime) {
        this.lastSwingTime = lastSwingTime;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getPing() {
        return ping;
    }

    public void setPing(long ping) {
        this.ping = ping;
    }

    public String getClientBrand() {
        return clientBrand;
    }

    public void setClientBrand(String clientBrand) {
        this.clientBrand = clientBrand;
    }

    public FixedSizeQueue<Long> getClickIntervals() {
        return clickIntervals;
    }

    public FixedSizeQueue<Float> getYawDeltas() {
        return yawDeltas;
    }

    public FixedSizeQueue<Float> getPitchDeltas() {
        return pitchDeltas;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void addConfidence(double amount) {
        this.confidenceScore = Math.min(1.0, this.confidenceScore + amount);
    }

    public void decayConfidence(double decay) {
        this.confidenceScore = Math.max(0.0, this.confidenceScore - decay);
    }

    public Map<String, Integer> getViolationLevels() {
        return violationLevels;
    }

    public void addViolation(String checkName, int weight) {
        violationLevels.merge(checkName, weight, Integer::sum);
    }

    /**
     * Internal FixedSizeQueue for circular sampling.
     */
    public static class FixedSizeQueue<T> extends LinkedList<T> {
        private final int maxSize;

        public FixedSizeQueue(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public boolean add(T k) {
            if (size() >= maxSize) {
                removeFirst();
            }
            return super.add(k);
        }
        
        @Override
        public void addLast(T k) {
            if (size() >= maxSize) {
                removeFirst();
            }
            super.addLast(k);
        }
    }

    /**
     * Representation of an entity's movement state for lag compensation/backtrack analysis.
     */
    public static class EntityState {
        private final double x, y, z;
        private final long timestamp;

        public EntityState(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = System.currentTimeMillis();
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public long getTimestamp() { return timestamp; }
    }
}
