package fr.lampalon.lifemod.common.nms.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Interface for NMS operations required by the Replay system.
 */
public interface NMSReplayHandler {

    /**
     * Spawns a virtual player NPC.
     * @param spectator The mod viewing the replay.
     * @param uuid NPC unique ID.
     * @param name NPC name.
     * @param location Spawn location.
     */
    void spawnNPC(Player spectator, UUID uuid, String name, Location location);

    /**
     * Removes a virtual player NPC.
     * @param spectator The mod viewing the replay.
     * @param uuid NPC unique ID.
     */
    void removeNPC(Player spectator, UUID uuid);
}
