package fr.lampalon.lifemod.common.replay;

import fr.lampalon.lifemod.common.replay.buffer.ReplayBuffer;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import fr.lampalon.lifemod.common.replay.storage.ReplayWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class ReplaySession {
    private static final Logger LOGGER = Logger.getLogger("ReplaySession");

    public static final int MOVEMENT_INTERVAL = 10;

    private final UUID playerUUID;
    private final int entityId;
    private final String playerName;
    private final String sessionName;
    private String worldName;
    private final ReplayBuffer buffer;
    private final ReplayWriter writer;
    private volatile boolean recording;
    private volatile boolean stopped;
    private double startX, startY, startZ;
    private float startYaw, startPitch;
    private final Set<Integer> activeEntities = Collections.synchronizedSet(new HashSet<>());
    private final Set<Integer> recordablePlayers = Collections.synchronizedSet(new HashSet<>());
    private final Set<Integer> movedThisTick = Collections.synchronizedSet(new HashSet<>());
    private final List<byte[]> pendingPackets = new ArrayList<>();
    private volatile int tickCounter = 0;

    public ReplaySession(UUID playerUUID, int entityId, String playerName, String sessionName, ReplayWriter writer) {
        if (playerUUID == null) throw new IllegalArgumentException("playerUUID cannot be null");
        if (playerName == null) throw new IllegalArgumentException("playerName cannot be null");
        if (sessionName == null) throw new IllegalArgumentException("sessionName cannot be null");
        if (writer == null) throw new IllegalArgumentException("writer cannot be null");
        this.playerUUID = playerUUID;
        this.entityId = entityId;
        this.playerName = playerName;
        this.sessionName = sessionName;
        this.buffer = new ReplayBuffer();
        this.writer = writer;
    }

    public void start() {
        if (recording) {
            LOGGER.warning("Double-start prevented for session: " + sessionName);
            return;
        }
        LOGGER.fine("Starting session: " + sessionName + " for " + playerUUID);
        if (writer == null) {
            LOGGER.severe("Cannot start session " + sessionName + ": writer is null");
            return;
        }
        if (playerUUID == null) {
            LOGGER.severe("Cannot start session " + sessionName + ": playerUUID is null");
            return;
        }
        this.writer.initialize(sessionName);
        this.writer.writeHeader(playerUUID, entityId, playerName, worldName, startX, startY, startZ, startYaw, startPitch);
        this.recording = true;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }

    public void stop() {
        if (stopped) return;
        stopped = true;
        this.recording = false;
        flushFrame();
        if (buffer == null) {
            if (writer != null) writer.close();
            return;
        }
        List<ReplayFrame> allFrames = buffer.getFrames(3600000L);
        if (allFrames != null && !allFrames.isEmpty()) {
            try {
                writer.writeFrames(allFrames);
            } catch (Exception e) {
                LOGGER.severe("Failed to write frames for " + sessionName + ": " + e.getMessage());
            } finally {
                try { writer.close(); } catch (Exception ignored) {}
            }
        } else {
            try { writer.close(); } catch (Exception ignored) {}
        }
    }

    public void queuePacket(byte[] data) {
        if (!recording || data == null || data.length == 0) return;
        synchronized (pendingPackets) {
            pendingPackets.add(data);
        }
    }

    public void flushFrame() {
        if (!recording) return;
        List<byte[]> packets;
        synchronized (pendingPackets) {
            if (pendingPackets.isEmpty()) return;
            packets = new ArrayList<>(pendingPackets);
            pendingPackets.clear();
        }
        addFrame(new ReplayFrame(System.currentTimeMillis(), Collections.unmodifiableList(packets)));
    }

    public void addFrame(ReplayFrame frame) {
        if (!recording || frame == null || buffer == null) return;
        buffer.addFrame(frame);
        movedThisTick.clear();
        tickCounter++;
    }

    public boolean isMovementTick() {
        return tickCounter % MOVEMENT_INTERVAL == 0;
    }

    public void trackSpawn(int entityId) {
        activeEntities.add(entityId);
    }

    public void trackDestroy(int entityId) {
        activeEntities.remove(entityId);
    }

    public boolean isEntityActive(int entityId) {
        return activeEntities.contains(entityId);
    }

    public void trackRecordablePlayer(int entityId) {
        recordablePlayers.add(entityId);
    }

    public void untrackRecordablePlayer(int entityId) {
        recordablePlayers.remove(entityId);
    }

    public boolean isRecordable(int entityId) {
        return entityId == this.entityId || recordablePlayers.contains(entityId);
    }

    public Set<Integer> getRecordablePlayers() {
        return recordablePlayers;
    }

    public boolean markMoved(int entityId) {
        return movedThisTick.add(entityId);
    }

    public List<ReplayFrame> getRecordedData(long durationMs) {
        if (buffer == null) return Collections.emptyList();
        long actualDuration = Math.min(durationMs <= 0 ? 60000 : durationMs, 3600000L);
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

    public UUID getPlayerUUID() { return playerUUID; }
    public int getEntityId() { return entityId; }
    public String getPlayerName() { return playerName; }
    public String getSessionName() { return sessionName; }
    public boolean isRecording() { return recording; }
    public ReplayBuffer getBuffer() { return buffer; }
}
