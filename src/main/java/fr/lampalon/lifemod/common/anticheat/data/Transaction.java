package fr.lampalon.lifemod.common.anticheat.data;

import java.util.function.Consumer;

/**
 * Represents a network transaction for client-server state synchronization.
 */
public final class Transaction {
    private final short id;
    private final long timestamp;
    private final Consumer<ACPlayerData> callback;

    public Transaction(short id, Consumer<ACPlayerData> callback) {
        this.id = id;
        this.timestamp = System.currentTimeMillis();
        this.callback = callback;
    }

    public short getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void execute(ACPlayerData data) {
        if (callback != null) {
            callback.accept(data);
        }
    }
}
