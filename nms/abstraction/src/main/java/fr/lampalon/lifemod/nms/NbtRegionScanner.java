package fr.lampalon.lifemod.nms;

import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import org.bukkit.Bukkit;

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@FunctionalInterface
interface NbtReader {
    Object read(DataInputStream input) throws Exception;
}

public final class NbtRegionScanner {

    private static final Set<String> CONTAINER_IDS = Set.of(
            "minecraft:chest", "minecraft:trapped_chest",
            "minecraft:barrel", "minecraft:hopper",
            "minecraft:dispenser", "minecraft:dropper",
            "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker",
            "minecraft:shulker_box"
    );

    private NbtRegionScanner() {
    }

    public static List<NMSProvider.ChunkItemHit> scan(InputStream chunkData, String targetMaterial,
                                                      String targetIAId, NbtReader reader) {
        List<NMSProvider.ChunkItemHit> hits = new ArrayList<>();
        try {
            DataInputStream dataInput = new DataInputStream(chunkData);
            Object chunkNBT = reader.read(dataInput);
            if (chunkNBT == null) return hits;

            Method getListMethod = chunkNBT.getClass().getMethod("getList", String.class, int.class);
            Method getStringMethod = chunkNBT.getClass().getMethod("getString", String.class);
            Method getIntMethod = chunkNBT.getClass().getMethod("getInt", String.class);
            Method containsMethod = chunkNBT.getClass().getMethod("contains", String.class);
            Method getCompoundMethod = chunkNBT.getClass().getMethod("getCompound", String.class);

            Object blockEntities = getListMethod.invoke(chunkNBT, "block_entities", 10);
            if (blockEntities == null) return hits;

            Method sizeMethod = blockEntities.getClass().getMethod("size");
            Method beGetCompound = blockEntities.getClass().getMethod("getCompound", int.class);
            int size = (int) sizeMethod.invoke(blockEntities);

            for (int i = 0; i < size; i++) {
                Object be = beGetCompound.invoke(blockEntities, i);
                String beId = (String) getStringMethod.invoke(be, "id");
                if (!CONTAINER_IDS.contains(beId)) continue;

                int beX = (int) getIntMethod.invoke(be, "x");
                int beY = (int) getIntMethod.invoke(be, "y");
                int beZ = (int) getIntMethod.invoke(be, "z");

                if (!(boolean) containsMethod.invoke(be, "Items")) continue;

                Object items = getListMethod.invoke(be, "Items", 10);
                int count = countItemsInList(items, targetMaterial, targetIAId,
                        containsMethod, getStringMethod, getCompoundMethod);
                if (count > 0) {
                    hits.add(new NMSProvider.ChunkItemHit(beX, beY, beZ, count));
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[NMS] scanChunkItems error: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
        return hits;
    }

    private static int countItemsInList(Object items, String targetMaterial, String targetIAId,
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

    private static String extractIAId(Object item, Method containsMethod, Method getStringMethod, Method getCompoundMethod) {
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
            Bukkit.getLogger().warning("extractIAId error: " + e.getMessage());
        }
        return null;
    }
}
