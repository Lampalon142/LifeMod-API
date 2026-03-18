package fr.lampalon.lifemod.common.replay.packet;

import java.util.List;

/**
 * Represents a single tick of recorded data.
 */
public class ReplayFrame {

    private final long timestamp;
    private final List<Object> packets;

    /**
     * Constructs a ReplayFrame.
     * @param timestamp The relative timestamp in milliseconds.
     * @param packets The list of packets captured during this tick.
     */
    public ReplayFrame(long timestamp, List<Object> packets) {
        this.timestamp = timestamp;
        this.packets = packets;
    }

    /**
     * @return The timestamp of this frame.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return The captured packets.
     */
    public List<Object> getPackets() {
        return packets;
    }
}
