package fr.lampalon.lifemod.platform.bukkit.managers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatContextManager {

    private final int limit;
    private final Map<UUID, Deque<StoredMessage>> messages = new ConcurrentHashMap<>();

    public ChatContextManager(int limit) {
        this.limit = Math.max(1, limit);
    }

    public void record(UUID uuid, String name, String message, long timestamp) {
        Deque<StoredMessage> deque = messages.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(new StoredMessage(name, message, timestamp));
            while (deque.size() > limit) deque.removeFirst();
        }
    }

    public List<StoredMessage> getRecent(UUID uuid) {
        Deque<StoredMessage> deque = messages.get(uuid);
        if (deque == null) return List.of();
        synchronized (deque) {
            return new ArrayList<>(deque);
        }
    }

    public void clear(UUID uuid) {
        messages.remove(uuid);
    }

    public static final class StoredMessage {
        private final String name;
        private final String message;
        private final long timestamp;

        public StoredMessage(String name, String message, long timestamp) {
            this.name = name;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getName() { return name; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}