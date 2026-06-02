package fr.lampalon.lifemod.common.replay.packet;

import java.util.List;

public class ReplayFrame {
    private final long timestamp;
    private final List<byte[]> packets;

    public ReplayFrame(long timestamp, List<byte[]> packets) {
        this.timestamp = timestamp;
        this.packets = packets;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<byte[]> getPackets() {
        return packets;
    }
}
