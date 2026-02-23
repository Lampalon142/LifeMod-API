package fr.lampalon.lifemod.common.anticheat.check;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import java.util.UUID;

/**
 * Interface representing a generic detection check (Combat, Movement, World).
 */
public interface Check {

    /**
     * Called when the player sends a packet.
     */
    void onHandle(UUID uuid, ACPlayerData data, Object context);

    /**
     * @return The packet types this check is interested in.
     */
    PacketTypeCommon[] getSupportedPackets();

    /**
     * @return Whether this check should be executed asynchronously.
     */
    boolean isAsync();

    /**
     * @return The unique name of the check.
     */
    String getName();

    /**
     * @return Whether the check is enabled.
     */
    boolean isEnabled();

    /**
     * Flag the player for a violation.
     */
    void flag(UUID uuid, ACPlayerData data, double probability, String details);
}
