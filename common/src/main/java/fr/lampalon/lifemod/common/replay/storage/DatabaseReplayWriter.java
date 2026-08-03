package fr.lampalon.lifemod.common.replay.storage;

import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class DatabaseReplayWriter implements ReplayWriter {

    private static final Logger LOGGER = Logger.getLogger("DatabaseReplayWriter");

    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ReplayDBWriter");
        t.setDaemon(true);
        return t;
    });

    private final DatabaseProvider db;
    private String sessionName;
    private UUID playerUuid;
    private int entityId;
    private String playerName;
    private String worldName;
    private double startX, startY, startZ;
    private float startYaw, startPitch;

    public DatabaseReplayWriter(DatabaseProvider db) {
        if (db == null) throw new IllegalArgumentException("db cannot be null");
        this.db = db;
    }

    @Override
    public void initialize(String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            LOGGER.severe("initialize: invalid sessionName");
            return;
        }
        this.sessionName = sessionName;
    }

    @Override
    public void writeHeader(UUID playerUUID, int entityId, String playerName, String worldName,
                            double x, double y, double z, float yaw, float pitch) {
        if (playerUUID == null) {
            LOGGER.severe("writeHeader: playerUUID is null");
            return;
        }
        this.playerUuid = playerUUID;
        this.entityId = entityId;
        this.playerName = playerName != null ? playerName : "unknown";
        this.worldName = worldName;
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.startYaw = yaw;
        this.startPitch = pitch;
    }

    @Override
    public void writeFrames(List<ReplayFrame> frames) {
        if (sessionName == null || playerUuid == null) {
            LOGGER.severe("writeFrames: not initialized");
            return;
        }
        if (frames == null || frames.isEmpty()) {
            LOGGER.warning("writeFrames: no frames to write");
            return;
        }
        ReplayFormat.SerializedReplay serialized = ReplayFormat.serialize(
                playerUuid, entityId, playerName, startX, startY, startZ, startYaw, startPitch, frames);
        if (serialized.frameCount == 0) {
            LOGGER.warning("writeFrames: no serializable frames for " + sessionName);
            return;
        }
        byte[] data = ReplayFormat.toEnvelope(serialized.payload);
        long durationMs = serialized.lastTs > serialized.firstTs
                ? (serialized.lastTs - serialized.firstTs) : 30000;
        final String name = sessionName;
        final int frameCount = serialized.frameCount;
        final long duration = durationMs;
        final byte[] blob = data;

        if (db.supportsAsyncWrites()) {
            SAVE_EXECUTOR.submit(() -> save(name, frameCount, duration, blob));
        } else {
            save(name, frameCount, duration, blob);
        }
    }

    private void save(String name, int frameCount, long durationMs, byte[] blob) {
        try {
            db.saveReplay(name, playerUuid, entityId, playerName, worldName,
                    startX, startY, startZ, startYaw, startPitch, durationMs, frameCount, blob);
            LOGGER.fine("Saved replay " + name + ": " + frameCount + " frames, "
                    + blob.length + " bytes (compressed)");
        } catch (Exception e) {
            LOGGER.severe("Failed to save replay " + name + " to database: " + e.getMessage());
        }
    }

    @Override
    public void close() {
    }
}
