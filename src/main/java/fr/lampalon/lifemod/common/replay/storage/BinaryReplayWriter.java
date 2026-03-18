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
                        com.github.retrooper.packetevents.netty.buffer.ByteBufHelper helper = com.github.retrooper.packetevents.PacketEvents.getAPI().getNettyManager().getByteBufHelper();
                        com.github.retrooper.packetevents.netty.buffer.ByteBuf buffer = helper.allocate(1024);
                        com.github.retrooper.packetevents.PacketEvents.getAPI().getProtocolManager().writePacket(buffer, (com.github.retrooper.packetevents.protocol.packetwrapper.PacketWrapper<?>) packet);
                        byte[] bytes = new byte[buffer.readableBytes()];
                        buffer.readBytes(bytes);
                        outputStream.writeInt(bytes.length);
                        outputStream.write(bytes);
                        buffer.release();
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
