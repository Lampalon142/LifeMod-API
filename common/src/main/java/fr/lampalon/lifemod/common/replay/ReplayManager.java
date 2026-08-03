package fr.lampalon.lifemod.common.replay;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.replay.storage.BinaryReplayWriter;
import fr.lampalon.lifemod.common.replay.storage.DatabaseReplayWriter;
import fr.lampalon.lifemod.common.replay.storage.ReplayWriter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ReplayManager {

    private static final Logger LOGGER = Logger.getLogger("ReplayManager");

    private final Map<UUID, ReplaySession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Integer, ReplaySession> entityIdSessions = new ConcurrentHashMap<>();
    private final SkinManager skinManager = new SkinManager();
    private final boolean useDatabase;

    public ReplayManager() {
        DatabaseProvider db = null;
        try {
            db = ServiceRegistry.get(DatabaseProvider.class);
        } catch (Exception ignored) {}
        this.useDatabase = db != null;
        if (useDatabase) {
            LOGGER.info("ReplayManager: database storage enabled");
        } else {
            LOGGER.info("ReplayManager: file-based storage (no database)");
        }
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    public void startRecording(UUID playerUUID, int entityId, String playerName, String sessionName,
                                String worldName, double x, double y, double z, float yaw, float pitch) {
        if (playerUUID == null) {
            LOGGER.severe("startRecording: playerUUID is null");
            return;
        }
        if (activeSessions.containsKey(playerUUID)) {
            LOGGER.warning("startRecording: already recording for " + playerUUID);
            return;
        }
        if (sessionName == null || sessionName.isEmpty()) {
            LOGGER.severe("startRecording: sessionName is invalid");
            return;
        }
        ReplayWriter writer = createWriter();
        if (writer == null) {
            LOGGER.severe("startRecording: failed to create writer");
            return;
        }
        ReplaySession session = new ReplaySession(playerUUID, entityId, playerName, sessionName, writer);
        session.setWorldName(worldName);
        session.setStartPosition(x, y, z, yaw, pitch);
        session.start();
        activeSessions.put(playerUUID, session);
        entityIdSessions.put(entityId, session);
    }

    private ReplayWriter createWriter() {
        try {
            if (useDatabase) {
                DatabaseProvider db = ServiceRegistry.get(DatabaseProvider.class);
                if (db != null) return new DatabaseReplayWriter(db);
                LOGGER.warning("Database provider not available, falling back to file storage");
            }
            return new BinaryReplayWriter();
        } catch (Exception e) {
            LOGGER.severe("Failed to create replay writer: " + e.getMessage());
            return new BinaryReplayWriter();
        }
    }

    public void cleanupExpiredReplays() {
        if (!useDatabase) return;
        try {
            DatabaseProvider db = ServiceRegistry.get(DatabaseProvider.class);
            if (db != null) {
                long oneHourAgo = System.currentTimeMillis() - 3600000L;
                db.deleteExpiredReplays(oneHourAgo);
                LOGGER.info("Cleaned up expired replays older than 1 hour");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to cleanup expired replays: " + e.getMessage());
        }
    }

    public void stopRecording(UUID playerUUID) {
        if (playerUUID == null) return;
        ReplaySession session = activeSessions.remove(playerUUID);
        if (session != null) {
            try {
                session.stop();
            } catch (Exception e) {
                LOGGER.severe("Error stopping session for " + playerUUID + ": " + e.getMessage());
            }
            entityIdSessions.values().removeIf(s -> s.equals(session));
        }
    }

    public ReplaySession getSession(UUID playerUUID) {
        if (playerUUID == null) return null;
        return activeSessions.get(playerUUID);
    }

    public ReplaySession getSessionByEntityId(int entityId) {
        return entityIdSessions.get(entityId);
    }

    public boolean isRecording(UUID playerUUID) {
        if (playerUUID == null) return false;
        return activeSessions.containsKey(playerUUID);
    }
}
