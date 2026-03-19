package fr.lampalon.lifemod.common.replay;

import fr.lampalon.lifemod.common.replay.buffer.ReplayBuffer;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages the recording of a player's session.
 * Keeps data in a circular buffer for a maximum of 1 hour.
 */
public class ReplaySession {
    private static final Logger LOGGER = Logger.getLogger("ReplaySession");
    private final UUID playerUUID;
    private final int entityId;
    private final String playerName;
    private final String sessionName;
    private final ReplayBuffer buffer;
    private final ScheduledExecutorService scheduler;
    private boolean recording;
    private double startX, startY, startZ;
    private float startYaw, startPitch;

    public ReplaySession(UUID playerUUID, int entityId, String playerName, String sessionName) {
        this.playerUUID = playerUUID;
        this.entityId = entityId;
        this.playerName = playerName;
        this.sessionName = sessionName;
        this.buffer = new ReplayBuffer();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts the recording session.
     */
    public void start() {
        LOGGER.info("[DEBUG] Starting session: " + sessionName + " for " + playerUUID);
        this.recording = true;

        // Regular cleanup to ensure memory is managed even if no frames are added
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (recording) {
                    // ReplayBuffer already handles cleanup on frame addition, 
                    // but we can force it here for safety.
                    // Since it has no public cleanup(), it's fine for now.
                }
            } catch (Exception e) {
                LOGGER.severe("[DEBUG] Error during cleanup for " + sessionName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Stops the recording session.
     */
    public void stop() {
        LOGGER.info("[DEBUG] Stopping session: " + sessionName);
        this.recording = false;
        scheduler.shutdown();
        buffer.clear();
    }

    /**
     * Adds a frame to the session.
     */
    public void addFrame(ReplayFrame frame) {
        if (recording) {
            buffer.addFrame(frame);
        } else {
            LOGGER.warning("[DEBUG] Attempted to add frame to session " + sessionName + " but recording is false.");
        }
    }

    /**
     * Retrieves recorded data for a specific duration.
     * @param durationMs Duration in milliseconds (max 1 hour / 3600000ms).
     * @return List of frames for the requested duration.
     */
    public List<ReplayFrame> getRecordedData(long durationMs) {
        // Ensure duration doesn't exceed 1 hour
        long actualDuration = Math.min(durationMs, 3600000L);
        return buffer.getFrames(actualDuration);
    }

    public void setStartPosition(double x, double y, double z, float yaw, float pitch) {
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.startYaw = yaw;
        this.startPitch = pitch;
    }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getStartZ() { return startZ; }
    public float getStartYaw() { return startYaw; }
    public float getStartPitch() { return startPitch; }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public int getEntityId() {
        return entityId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getSessionName() {
        return sessionName;
    }

    public boolean isRecording() {
        return recording;
    }
    
    public ReplayBuffer getBuffer() {
        return buffer;
    }
}
