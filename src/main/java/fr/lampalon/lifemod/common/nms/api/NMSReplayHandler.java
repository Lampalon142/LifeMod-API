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
     * @param entityId Explicit entity ID to use.
     * @param uuid NPC unique ID.
     * @param name NPC name.
     * @param skin Skin properties (textures/signature).
     * @param location Spawn location.
     */
    void spawnNPC(Player spectator, int entityId, UUID uuid, String name, com.github.retrooper.packetevents.protocol.player.TextureProperty[] skin, org.bukkit.Location location);


    /**
     * Removes a virtual player NPC.
     * @param spectator The mod viewing the replay.
     * @param entityId NPC entity ID.
     * @param uuid NPC unique ID.
     */
    void removeNPC(Player spectator, int entityId, java.util.UUID uuid);

}
