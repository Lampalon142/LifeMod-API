package fr.lampalon.lifemod.common.anticheat.data;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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

    // Combat Samples (Sliding windows) - Thread-safe structure
    private final FixedSizeQueue<Long> clickIntervals = new FixedSizeQueue<>(100);
    private final Map<Integer, EntityState> entityStates = new ConcurrentHashMap<>();

    // Rotation Samples
    private final FixedSizeQueue<Float> yawDeltas = new FixedSizeQueue<>(50);
    private final FixedSizeQueue<Float> pitchDeltas = new FixedSizeQueue<>(50);

    // Timer Balance
    private long timerBalance = 0;
    private long lastMoveTime = -1;
    private long lastSwingTime = -1;
    private long highCpsStartTime = -1;
    private double hitboxBuffer = 0.0;

    // View
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private float lastDeltaYaw = 0.0f;
    private float lastDeltaPitch = 0.0f;
    private double lastGcd = 0.0;
    private double aimbotSnapBuffer = 0.0;
    private double aimbotSmoothBuffer = 0.0;
    private double aimbotJitterBuffer = 0.0;
    private long lastAttackTime = 0;
    private int lastTargetId = -1;
    private double headLockBuffer = 0.0;
    private float lastRatio = 0.0f;



    private boolean onGround = true;
    private boolean lastOnGround = true;
    private int airTicks = 0;
    private int groundTicks = 0;
    private long lastVelocityTime = 0;
    private double lastY = 0.0;
    private double deltaY = 0.0;
    private float fallDistance = 0.0f;


    // Rotation Samples (last 20 deltas for statistical analysis)
    private final FixedSizeQueue<Float> lastYawDeltas = new FixedSizeQueue<>(20);
    private final FixedSizeQueue<Float> lastPitchDeltas = new FixedSizeQueue<>(20);
    private final FixedSizeQueue<Float> lastAccelerations = new FixedSizeQueue<>(20);

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

    public long getHighCpsStartTime() {
        return highCpsStartTime;
    }

    public void setHighCpsStartTime(long highCpsStartTime) {
        this.highCpsStartTime = highCpsStartTime;
    }

    public double getHitboxBuffer() {
        return hitboxBuffer;
    }

    public void setHitboxBuffer(double hitboxBuffer) {
        this.hitboxBuffer = hitboxBuffer;
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

    public double getAimbotSmoothBuffer() {
        return aimbotSmoothBuffer;
    }

    public void setAimbotSmoothBuffer(double aimbotSmoothBuffer) {
        this.aimbotSmoothBuffer = aimbotSmoothBuffer;
    }

    public double getAimbotSnapBuffer() {
        return aimbotSnapBuffer;
    }

    public void setAimbotSnapBuffer(double aimbotSnapBuffer) {
        this.aimbotSnapBuffer = aimbotSnapBuffer;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }

    public float getLastDeltaYaw() {
        return lastDeltaYaw;
    }

    public void setLastDeltaYaw(float lastDeltaYaw) {
        this.lastDeltaYaw = lastDeltaYaw;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }

    public int getLastTargetId() { return lastTargetId; }
    public void setLastTargetId(int lastTargetId) { this.lastTargetId = lastTargetId; }

    public double getHeadLockBuffer() { return headLockBuffer; }
    public void setHeadLockBuffer(double headLockBuffer) { this.headLockBuffer = headLockBuffer; }

    public float getLastDeltaPitch() { return lastDeltaPitch; }
    public void setLastDeltaPitch(float lastDeltaPitch) { this.lastDeltaPitch = lastDeltaPitch; }

    public double getLastGcd() { return lastGcd; }
    public void setLastGcd(double lastGcd) { this.lastGcd = lastGcd; }

    public double getAimbotJitterBuffer() { return aimbotJitterBuffer; }
    public void setAimbotJitterBuffer(double aimbotJitterBuffer) { this.aimbotJitterBuffer = aimbotJitterBuffer; }

    public FixedSizeQueue<Float> getLastYawDeltas() { return lastYawDeltas; }
    public FixedSizeQueue<Float> getLastPitchDeltas() { return lastPitchDeltas; }
    public FixedSizeQueue<Float> getLastAccelerations() { return lastAccelerations; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    public boolean isLastOnGround() { return lastOnGround; }
    public void setLastOnGround(boolean lastOnGround) { this.lastOnGround = lastOnGround; }

    public int getAirTicks() { return airTicks; }
    public void setAirTicks(int airTicks) { this.airTicks = airTicks; }

    public int getGroundTicks() { return groundTicks; }
    public void setGroundTicks(int groundTicks) { this.groundTicks = groundTicks; }

    public long getLastVelocityTime() { return lastVelocityTime; }
    public void setLastVelocityTime(long lastVelocityTime) { this.lastVelocityTime = lastVelocityTime; }

    public double getLastY() { return lastY; }
    public void setLastY(double lastY) { this.lastY = lastY; }

    public double getDeltaY() { return deltaY; }
    public void setDeltaY(double deltaY) { this.deltaY = deltaY; }

    public float getFallDistance() { return fallDistance; }
    public void setFallDistance(float fallDistance) { this.fallDistance = fallDistance; }

    public float getLastRatio() {
        return lastRatio;
    }
    public void setLastRatio(float lastRatio) {
        this.lastRatio = lastRatio;
    }

    /**
     * Internal FixedSizeQueue for circular sampling.
     * Uses ConcurrentLinkedDeque to avoid ConcurrentModificationException.
     */
    public static class FixedSizeQueue<T> extends ConcurrentLinkedDeque<T> {
        private final int maxSize;

        public FixedSizeQueue(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public boolean add(T k) {
            if (size() >= maxSize) {
                pollFirst();
            }
            return super.add(k);
        }
        
        @Override
        public void addLast(T k) {
            if (size() >= maxSize) {
                pollFirst();
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
