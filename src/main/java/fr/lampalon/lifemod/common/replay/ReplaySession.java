package fr.lampalon.lifemod.common.replay;

import fr.lampalon.lifemod.common.replay.buffer.ReplayBuffer;
import fr.lampalon.lifemod.common.replay.storage.BinaryReplayWriter;
import fr.lampalon.lifemod.common.replay.storage.ReplayWriter;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ReplaySession {
    private static final Logger LOGGER = Logger.getLogger("ReplaySession");
    private final UUID playerUUID;
    private final String sessionName;
    private final ReplayBuffer buffer;
    private final ReplayWriter writer;
    private final ScheduledExecutorService scheduler;
    private boolean recording;

    public ReplaySession(UUID playerUUID, String sessionName) {
        this.playerUUID = playerUUID;
        this.sessionName = sessionName;
        this.buffer = new ReplayBuffer();
        this.writer = new BinaryReplayWriter();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        LOGGER.info("[DEBUG] Starting session: " + sessionName + " for " + playerUUID);
        this.writer.initialize(sessionName);
        this.recording = true;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (recording) {
                    var flushed = buffer.flush();
                    if (!flushed.isEmpty()) {
                        LOGGER.info("[DEBUG] Session " + sessionName + ": Flushing " + flushed.size() + " frames to disk.");
                        writer.writeFrames(flushed);
                    }
                }
            } catch (Exception e) {
                LOGGER.severe("[DEBUG] Error during flush for " + sessionName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        LOGGER.info("[DEBUG] Stopping session: " + sessionName);
        this.recording = false;
        scheduler.shutdown();
        writer.close();
    }

    public void addFrame(fr.lampalon.lifemod.common.replay.packet.ReplayFrame frame) {
        if (recording) {
            buffer.addFrame(frame);
        } else {
            LOGGER.warning("[DEBUG] Attempted to add frame to session " + sessionName + " but recording is false.");
        }
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getSessionName() {
        return sessionName;
    }

    public boolean isRecording() {
        return recording;
    }
}
