package fr.lampalon.lifemod.platform.bukkit.nms.v1_20_R1;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTitle;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation for NMS capabilities using PacketEvents (v1_20_R1 compatible).
 */
public class NMSHandler_v1_20_R1 implements NMSProvider {

    @Override
    public void sendActionBar(Player player, String message) {
        WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(Component.text(MessageUtil.formatMessage(message)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComp = title != null ? Component.text(MessageUtil.formatMessage(title)) : null;
        Component subtitleComp = subtitle != null ? Component.text(MessageUtil.formatMessage(subtitle)) : null;

        WrapperPlayServerTitle timePacket = new WrapperPlayServerTitle(
                WrapperPlayServerTitle.TitleAction.SET_TIMES_AND_DISPLAY,
                (Component) null, (Component) null, (Component) null,
                fadeIn, stay, fadeOut
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, timePacket);

        if (titleComp != null) {
            WrapperPlayServerTitle titlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_TITLE,
                    titleComp, null, null,
                    fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, titlePacket);
        }

        if (subtitleComp != null) {
            WrapperPlayServerTitle subtitlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_SUBTITLE,
                    null, subtitleComp, null,
                    fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, subtitlePacket);
        }
    }

    @Override
    public void kickPlayer(Player player, String reason) {
        WrapperPlayServerDisconnect packet = new WrapperPlayServerDisconnect(Component.text(MessageUtil.formatMessage(reason)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
    public int getPing(Player player) {
        return player.getPing();
    }

    @Override
    public String getName() {
        return "v1_20_R1";
    }

    @Override
    public List<Container> getLoadedContainers(World world) {
        List<Container> containers = new ArrayList<>();

        // Only iterate already-loaded chunks
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
        List<ChunkItemHit> hits = new ArrayList<>();
        try {
            DataInputStream dataInput = new DataInputStream(chunkData);

            Class<?> nbtIoClass = Class.forName("net.minecraft.nbt.NbtIo");
            Class<?> nbtAccClass;
            Object chunkNBT;

            try {
                nbtAccClass = Class.forName("net.minecraft.nbt.NbtAccounter");
                Object accounter = nbtAccClass.getMethod("unlimitedHeap").invoke(null);
                chunkNBT = nbtIoClass.getMethod("read", java.io.DataInput.class, nbtAccClass)
                        .invoke(null, dataInput, accounter);
            } catch (Exception e) {
                chunkNBT = nbtIoClass.getMethod("read", java.io.DataInput.class)
                        .invoke(null, dataInput);
            }

            if (chunkNBT == null) return hits;

            Method getListMethod = chunkNBT.getClass().getMethod("getList", String.class, int.class);
            Method getStringMethod = chunkNBT.getClass().getMethod("getString", String.class);
            Method getIntMethod = chunkNBT.getClass().getMethod("getInt", String.class);
            Method containsMethod = chunkNBT.getClass().getMethod("contains", String.class);
            Method getCompoundMethod = chunkNBT.getClass().getMethod("getCompound", String.class);

            Object blockEntities = getListMethod.invoke(chunkNBT, "block_entities", 10);
            if (blockEntities == null) return hits;

            Method sizeMethod = blockEntities.getClass().getMethod("size");
            Method beGetCompoundMethod = blockEntities.getClass().getMethod("getCompound", int.class);
            int size = (int) sizeMethod.invoke(blockEntities);

            for (int i = 0; i < size; i++) {
                Object be = beGetCompoundMethod.invoke(blockEntities, i);
                String beId = (String) getStringMethod.invoke(be, "id");
                if (!CONTAINER_IDS.contains(beId)) continue;

                int beX = (int) getIntMethod.invoke(be, "x");
                int beY = (int) getIntMethod.invoke(be, "y");
                int beZ = (int) getIntMethod.invoke(be, "z");

                if (!(boolean) containsMethod.invoke(be, "Items")) continue;

                Object items = getListMethod.invoke(be, "Items", 10);
                int count = countItemsInList(items, targetMaterial, targetIAId, containsMethod, getStringMethod, getCompoundMethod);
                if (count > 0) {
                    hits.add(new ChunkItemHit(beX, beY, beZ, count));
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[NMS] scanChunkItems error: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
        return hits;
    }

    private int countItemsInList(Object items, String targetMaterial, String targetIAId,
                                  Method containsMethod, Method getStringMethod, Method getCompoundMethod) throws Exception {
        int count = 0;
        Method sizeMethod = items.getClass().getMethod("size");
        Method itemGetCompound = items.getClass().getMethod("getCompound", int.class);
        int size = (int) sizeMethod.invoke(items);

        for (int i = 0; i < size; i++) {
            Object item = itemGetCompound.invoke(items, i);
            String id = ((String) getStringMethod.invoke(item, "id"))
                    .replace("minecraft:", "").toUpperCase();

            int amount = 1;
            Method getByteMethod = item.getClass().getMethod("getByte", String.class);
            Method getIntMethod = item.getClass().getMethod("getInt", String.class);
            if ((boolean) containsMethod.invoke(item, "count")) {
                amount = (int) getIntMethod.invoke(item, "count");
            } else if ((boolean) containsMethod.invoke(item, "Count")) {
                amount = (byte) getByteMethod.invoke(item, "Count");
            }

            if (targetIAId != null) {
                String iaId = extractIAId(item, containsMethod, getStringMethod, getCompoundMethod);
                if (targetIAId.equals(iaId)) count += amount;
            } else {
                if (id.equals(targetMaterial)) count += amount;
            }
        }
        return count;
    }

    private String extractIAId(Object item, Method containsMethod, Method getStringMethod, Method getCompoundMethod) {
        try {
            if ((boolean) containsMethod.invoke(item, "components")) {
                Object components = getCompoundMethod.invoke(item, "components");
                Method compContains = components.getClass().getMethod("contains", String.class);
                Method compGetCompound = components.getClass().getMethod("getCompound", String.class);
                if ((boolean) compContains.invoke(components, "minecraft:custom_data")) {
                    Object customData = compGetCompound.invoke(components, "minecraft:custom_data");
                    Method cdContains = customData.getClass().getMethod("contains", String.class);
                    Method cdGetCompound = customData.getClass().getMethod("getCompound", String.class);
                    if ((boolean) cdContains.invoke(customData, "itemsadder")) {
                        Object iaTag = cdGetCompound.invoke(customData, "itemsadder");
                        return (String) getStringMethod.invoke(iaTag, "id");
                    }
                }
            }
            if ((boolean) containsMethod.invoke(item, "tag")) {
                Object tag = getCompoundMethod.invoke(item, "tag");
                Method tagContains = tag.getClass().getMethod("contains", String.class);
                Method tagGetCompound = tag.getClass().getMethod("getCompound", String.class);
                if ((boolean) tagContains.invoke(tag, "itemsadder")) {
                    Object iaTag = tagGetCompound.invoke(tag, "itemsadder");
                    return (String) getStringMethod.invoke(iaTag, "id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final Set<String> CONTAINER_IDS = Set.of(
            "minecraft:chest", "minecraft:trapped_chest",
            "minecraft:barrel", "minecraft:hopper",
            "minecraft:dispenser", "minecraft:dropper",
            "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker",
            "minecraft:shulker_box"
    );
}
