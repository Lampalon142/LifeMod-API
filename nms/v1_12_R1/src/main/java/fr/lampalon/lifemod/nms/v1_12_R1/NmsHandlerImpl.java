package fr.lampalon.lifemod.nms.v1_12_R1;

import fr.lampalon.lifemod.nms.AbstractNmsHandler;
import org.bukkit.plugin.Plugin;

import java.io.DataInputStream;

public final class NmsHandlerImpl extends AbstractNmsHandler {

    public NmsHandlerImpl(Plugin plugin) {
        super(plugin);
    }

    @Override
    protected Object readChunkNBT(DataInputStream input) throws Exception {
        Class<?> tools = Class.forName("net.minecraft.server.v1_12_R1.NBTCompressedStreamTools");
        return tools.getMethod("a", DataInputStream.class).invoke(null, input);
    }

    @Override
    public String getName() {
        return "v1_12_R1";
    }
}
