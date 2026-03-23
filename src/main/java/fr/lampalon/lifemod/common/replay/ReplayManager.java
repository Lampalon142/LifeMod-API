package fr.lampalon.lifemod.common.replay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple replay recording sessions for both real and fake players.
 */
public class ReplayManager {

    private final Map<UUID, ReplaySession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Integer, ReplaySession> entityIdSessions = new ConcurrentHashMap<>();
    private final SkinManager skinManager = new SkinManager();

    public SkinManager getSkinManager() {
        return skinManager;
    }

    /**
     * Starts recording a session.
     * @param playerUUID UUID of the player/bot.
     * @param entityId Entity ID of the player/bot.
     * @param sessionName Unique session name.
     */
    public void startRecording(UUID playerUUID, int entityId, String playerName, String sessionName, double x, double y, double z, float yaw, float pitch) {
        ReplaySession session = new ReplaySession(playerUUID, entityId, playerName, sessionName);
        session.setStartPosition(x, y, z, yaw, pitch);
        session.start();
        activeSessions.put(playerUUID, session);
        entityIdSessions.put(entityId, session);
    }

    /**
     * Stops the recording process.
     * @param playerUUID UUID of the player/bot.
     */
    public void stopRecording(UUID playerUUID) {
        ReplaySession session = activeSessions.remove(playerUUID);
        if (session != null) {
            session.stop();
            // Clean mapping by EntityId
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
