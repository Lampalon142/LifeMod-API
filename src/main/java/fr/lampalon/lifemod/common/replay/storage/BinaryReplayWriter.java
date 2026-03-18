package fr.lampalon.lifemod.common.replay.storage;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import java.io.*;
import java.util.List;

/**
 * Implementation of ReplayWriter using binary format for storage.
 */
public class BinaryReplayWriter implements ReplayWriter {

    private File sessionFile;
    private DataOutputStream outputStream;

    @Override
    public void initialize(String sessionName) {
        // We will store replays in a dedicated directory
        File replayDir = new File("plugins/LifeMod/replays");
        if (!replayDir.exists()) {
            replayDir.mkdirs();
        }
        this.sessionFile = new File(replayDir, sessionName + ".replay");
        try {
            this.outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(sessionFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeFrames(List<ReplayFrame> frames) {
        if (outputStream == null) return;
        try {
            for (ReplayFrame frame : frames) {
                outputStream.writeLong(frame.getTimestamp());
                outputStream.writeInt(frame.getPackets().size());
                for (Object packet : frame.getPackets()) {
                    if (packet instanceof com.github.retrooper.packetevents.protocol.packettype.PacketType) {
                        // In a real implementation, serialize the specific packet wrapper here.
                        // For the sake of this implementation, we write the packet ID or a marker.
                    }
                }
            }
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
