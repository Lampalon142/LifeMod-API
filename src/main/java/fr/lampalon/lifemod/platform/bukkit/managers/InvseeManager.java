package fr.lampalon.lifemod.platform.bukkit.managers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InvseeManager {
    private final Map<UUID, UUID> viewing = new HashMap<>();

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
}
