package fr.lampalon.lifemod.api.replay;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IReplayService {

    boolean isRecording(UUID playerUuid);

    boolean isInReplay(UUID playerUuid);

    CompletableFuture<List<ReplayInfo>> listReplays(int limit);

    /**
     * Plays back the last {@code durationMs} milliseconds of a currently
     * recording player for the given moderator.
     */
    void startPlayback(Player moderator, UUID targetUuid, long durationMs);

    /**
     * Loads a stored replay (database first, then file) and plays it for the
     * given moderator.
     */
    void loadAndPlay(Player moderator, String sessionName);

    void stopPlayback(Player moderator);

    void exitReplay(Player moderator);

    void stopRecording(UUID playerUuid);
}