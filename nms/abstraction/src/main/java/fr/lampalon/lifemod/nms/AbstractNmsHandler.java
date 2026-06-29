package fr.lampalon.lifemod.nms;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTitle;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNmsHandler implements NMSProvider {

    protected final Plugin plugin;

    protected AbstractNmsHandler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendActionBar(Player player, String message) {
        WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(
                Component.text(TextUtil.format(message)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComp = title != null ? Component.text(TextUtil.format(title)) : null;
        Component subtitleComp = subtitle != null ? Component.text(TextUtil.format(subtitle)) : null;

        WrapperPlayServerTitle timePacket = new WrapperPlayServerTitle(
                WrapperPlayServerTitle.TitleAction.SET_TIMES_AND_DISPLAY,
                Component.empty(), Component.empty(), Component.empty(),
                fadeIn, stay, fadeOut
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, timePacket);

        if (titleComp != null) {
            WrapperPlayServerTitle titlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_TITLE,
                    titleComp, Component.empty(), Component.empty(),
                    fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, titlePacket);
        }

        if (subtitleComp != null) {
            WrapperPlayServerTitle subtitlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_SUBTITLE,
                    Component.empty(), subtitleComp, Component.empty(),
                    fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, subtitlePacket);
        }
    }

    @Override
    public void kickPlayer(Player player, String reason) {
        WrapperPlayServerDisconnect packet = new WrapperPlayServerDisconnect(
                Component.text(TextUtil.format(reason)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
    public int getPing(Player player) {
        return player.getPing();
    }

    @Override
    public List<Container> getLoadedContainers(World world) {
        List<Container> containers = new ArrayList<>();
        for (Chunk chunk : world.getLoadedChunks()) {
            try {
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Container container) {
                        containers.add(container);
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Scan] Erreur chunk: " + e.getMessage());
            }
        }
        return containers;
    }

    @Override
    public List<ChunkItemHit> scanChunkItems(InputStream chunkData, String targetMaterial, String targetIAId) {
        return NbtRegionScanner.scan(chunkData, targetMaterial, targetIAId, this::readChunkNBT);
    }

    protected Object readChunkNBT(DataInputStream input) throws Exception {
        Class<?> nbtIo = Class.forName("net.minecraft.nbt.NbtIo");
        try {
            Class<?> accounter = Class.forName("net.minecraft.nbt.NbtAccounter");
            return nbtIo.getMethod("read", DataInput.class, accounter)
                    .invoke(null, input, accounter.getMethod("unlimitedHeap").invoke(null));
        } catch (Exception e) {
            return nbtIo.getMethod("read", DataInput.class).invoke(null, input);
        }
    }
}
