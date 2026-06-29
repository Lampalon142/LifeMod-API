package fr.lampalon.lifemod.platform.bukkit.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InvseeManager {
    private final Map<UUID, UUID> viewing = new HashMap<>();
    private final Map<UUID, UUID> offlineViewing = new HashMap<>();
    private final Map<UUID, Long> offlineInvLocked = new HashMap<>();

    public void startViewing(Player viewer, Player target) {
        viewing.put(viewer.getUniqueId(), target.getUniqueId());
    }

    public void stopViewing(Player viewer) {
        viewing.remove(viewer.getUniqueId());
    }

    public UUID getTargetUUID(Player viewer) {
        return viewing.get(viewer.getUniqueId());
    }

    public boolean isViewing(Player viewer) {
        return viewing.containsKey(viewer.getUniqueId());
    }

    public void startOfflineViewing(Player viewer, UUID targetUuid) {
        offlineViewing.put(viewer.getUniqueId(), targetUuid);
        offlineInvLocked.put(targetUuid, System.currentTimeMillis());
    }

    public UUID stopOfflineViewing(Player viewer) {
        UUID target = offlineViewing.remove(viewer.getUniqueId());
        if (target != null) offlineInvLocked.remove(target);
        return target;
    }

    public UUID getOfflineTargetUUID(Player viewer) {
        return offlineViewing.get(viewer.getUniqueId());
    }

    public boolean isOfflineViewing(Player viewer) {
        return offlineViewing.containsKey(viewer.getUniqueId());
    }

    public boolean isOfflineInvLocked(UUID targetUuid) {
        return offlineInvLocked.containsKey(targetUuid);
    }

    public void releaseOfflineLock(UUID targetUuid) {
        offlineInvLocked.remove(targetUuid);
    }

    public void releaseForTarget(UUID targetUuid) {
        offlineViewing.entrySet().removeIf(e -> e.getValue().equals(targetUuid));
        offlineInvLocked.remove(targetUuid);
    }

    public long getLockTime(UUID targetUuid) {
        return offlineInvLocked.getOrDefault(targetUuid, 0L);
    }
}
