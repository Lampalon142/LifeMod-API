package fr.lampalon.lifemod.common.replay.storage;

import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class DatabaseReplayReader {

    private static final Logger LOGGER = Logger.getLogger("DatabaseReplayReader");
    private static final int MAX_PACKET_LENGTH = 50_000_000;
    private static final int MAX_FRAMES = 5_000_000;

    private final DatabaseProvider db;
    private final String sessionName;

    private UUID playerUUID;
    private int entityId;
    private String playerName;
    private double startX, startY, startZ;
    private float startYaw, startPitch;

    public DatabaseReplayReader(DatabaseProvider db, String sessionName) {
        if (db == null) throw new IllegalArgumentException("db cannot be null");
        if (sessionName == null) throw new IllegalArgumentException("sessionName cannot be null");
        this.db = db;
        this.sessionName = sessionName;
    }

    public List<ReplayFrame> readAllFrames() {
        List<ReplayFrame> frames = new ArrayList<>();
        if (db == null || sessionName == null || sessionName.isEmpty()) {
            LOGGER.severe("readAllFrames: invalid arguments");
            return frames;
        }
        byte[] raw;
        try {
            raw = db.getReplayData(sessionName);
        } catch (Exception e) {
            LOGGER.severe("readAllFrames: failed to get replay data for " + sessionName + ": " + e.getMessage());
            return frames;
        }
        if (raw == null || raw.length == 0) {
            LOGGER.warning("readAllFrames: no data found for " + sessionName);
            return frames;
        }
        ReplayFormat.ParsedReplay parsed = ReplayFormat.parse(raw, MAX_FRAMES, MAX_PACKET_LENGTH);
        if (parsed == null) {
            LOGGER.warning("readAllFrames: could not parse replay for " + sessionName);
            return frames;
        }
        this.playerUUID = parsed.playerUUID;
        this.entityId = parsed.entityId;
        this.playerName = parsed.playerName;
        this.startX = parsed.startX;
        this.startY = parsed.startY;
        this.startZ = parsed.startZ;
        this.startYaw = parsed.startYaw;
        this.startPitch = parsed.startPitch;
        return parsed.frames;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public int getEntityId() { return entityId; }
    public String getPlayerName() { return playerName; }
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getStartZ() { return startZ; }
    public float getStartYaw() { return startYaw; }
    public float getStartPitch() { return startPitch; }
}
