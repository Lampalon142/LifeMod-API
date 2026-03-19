package fr.lampalon.lifemod.common.core;

import java.util.UUID;

public interface ILifePlatform {

    /**
     * Broadcast a message to all players with a specific permission (or all if permission is null).
     */
    void broadcast(String message, String permission);

    /**
     * Kick a player from the server/network.
     */
    void kickPlayer(UUID uuid, String reason);

    /**
     * Send a message to a specific player.
     */
    void sendMessage(UUID uuid, String message);

    /**
     * Run a task on the main thread.
     */
    void runTask(Runnable runnable);

    /**
     * Run a task asynchronously.
     */
    void runTaskAsync(Runnable runnable);

    /**
     * Get the name of a player from their UUID.
     */
    String getPlayerName(UUID uuid);

    /**
     * Log an information message to the console.
     */
    void logInfo(String message);

    /**
     * Get the name of the current server (for network synchronization).
     */
    String getServerName();
    
    /**
     * Check if a player is online on the current platform.
     */
    boolean isPlayerOnline(UUID uuid);

    /**
     * Dispatch a command as the console.
     */
    void dispatchCommand(String command);

    /**
     * Get the NMS provider for the current platform.
     */
    fr.lampalon.lifemod.common.nms.api.NMSProvider getNmsProvider();
}
