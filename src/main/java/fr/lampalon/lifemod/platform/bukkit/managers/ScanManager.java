package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IItemsAdderService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.model.ScanResult;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ListTag;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ScanManager {
    private final LifeMod plugin;
    private final IItemsAdderService itemsAdderService;

    public ScanManager(LifeMod plugin) {
        this.plugin = plugin;
        this.itemsAdderService = ServiceRegistry.get(IItemsAdderService.class);
    }

    /**
     * Scans for a specific item based on its material and custom data.
     */
    public CompletableFuture<ScanResult> scan(String type, String target, ItemStack targetItem, Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            ScanResult result = new ScanResult();
            progressCallback.accept("§7Début du scan (" + type + ")...");

            if (type.equalsIgnoreCase("inventories") || type.equalsIgnoreCase("all")) {
                scanInventories(target, targetItem, result, progressCallback);
            }

            if (type.equalsIgnoreCase("enderchest") || type.equalsIgnoreCase("all")) {
                scanEnderchests(target, targetItem, result, progressCallback);
            }

            if (type.equalsIgnoreCase("map") || type.equalsIgnoreCase("all")) {
                scanMap(targetItem, result, progressCallback);
            }
            
            result.complete();
            return result;
        });
    }

    private void scanMap(ItemStack targetItem, ScanResult result, Consumer<String> progressCallback) {
        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);

        // Collecte des chunks DÉJÀ chargés sur le main thread (safe, rapide)
        CompletableFuture<Map<org.bukkit.World, List<Container>>> loadedFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Map<World, List<Container>> map = new HashMap<>();
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                List<Container> containers = new ArrayList<>();
                for (Chunk chunk : world.getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state instanceof Container c) containers.add(c);
                    }
                }
                map.put(world, containers);
            }
            loadedFuture.complete(map);
        });

        try {
            Map<org.bukkit.World, List<Container>> loadedContainers = loadedFuture.get(10, TimeUnit.SECONDS);

            // 1. Scan des chunks chargés (inventaire live)
            for (var entry : loadedContainers.entrySet()) {
                progressCallback.accept("§7Scan de §e" + entry.getValue().size() + "§7 conteneurs chargés dans §a" + entry.getKey().getName());
                for (Container container : entry.getValue()) {
                    int count = countItems(container.getInventory().getContents(), targetItem);
                    if (count > 0) result.addLocation(container.getLocation(), count);
                }
            }

            // 2. Scan des fichiers région (chunks non chargés) — ASYNC, pas de freeze
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                scanRegionFiles(world, targetItem, result, progressCallback);
            }

        } catch (Exception e) {
            progressCallback.accept("§cErreur durant le scan: " + e.getMessage());
        }
    }

    private void scanInventories(String target, ItemStack targetItem, ScanResult result, Consumer<String> progressCallback) {
        if (target.equalsIgnoreCase("all")) {
            progressCallback.accept("§7Scan des inventaires de tous les joueurs...");
            for (Player player : Bukkit.getOnlinePlayers()) {
                int count = countItems(player.getInventory().getContents(), targetItem);
                if (count > 0) result.addPlayer(player.getUniqueId(), count);
            }
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                progressCallback.accept("§7Scan de l'inventaire de " + player.getName() + "...");
                int count = countItems(player.getInventory().getContents(), targetItem);
                if (count > 0) result.addPlayer(player.getUniqueId(), count);
            }
        }
    }

    private void scanEnderchests(String target, ItemStack targetItem, ScanResult result, Consumer<String> progressCallback) {
        if (target.equalsIgnoreCase("all")) {
            progressCallback.accept("§7Scan des Enderchests de tous les joueurs...");
            for (Player player : Bukkit.getOnlinePlayers()) {
                int count = countItems(player.getEnderChest().getContents(), targetItem);
                if (count > 0) result.addPlayer(player.getUniqueId(), count);
            }
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                progressCallback.accept("§7Scan de l'Enderchest de " + player.getName() + "...");
                int count = countItems(player.getEnderChest().getContents(), targetItem);
                if (count > 0) result.addPlayer(player.getUniqueId(), count);
            }
        }
    }

    private int countItems(ItemStack[] contents, ItemStack targetItem) {
        int count = 0;
        if (contents == null) return 0;
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) continue;
            
            if (isMatch(item, targetItem)) {
                count += item.getAmount();
            }
            
            // Check for items inside containers (like Shulker Boxes)
            if (item.getType().name().contains("SHULKER_BOX")) {
                if (item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta meta) {
                    if (meta.getBlockState() instanceof org.bukkit.block.Container container) {
                        count += countItems(container.getInventory().getContents(), targetItem);
                    }
                }
            }
        }
        return count;
    }

    /**
     * Checks if two ItemStacks match.
     * Supports Materials, CustomModelData and ItemsAdder IDs.
     */
    public boolean isMatch(ItemStack item, ItemStack target) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (target == null || target.getType() == Material.AIR) return false;

        if (itemsAdderService != null && itemsAdderService.isEnabled()) {
            String itemId = itemsAdderService.getItemId(item);
            String targetId = itemsAdderService.getItemId(target);
            
            if (itemId != null || targetId != null) {
                return itemId != null && itemId.equals(targetId);
            }
        }

        if (item.getType() != target.getType()) return false;

        if (item.hasItemMeta() && target.hasItemMeta()) {
            if (item.getItemMeta().hasCustomModelData() != target.getItemMeta().hasCustomModelData()) return false;
            if (item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() != target.getItemMeta().getCustomModelData()) return false;
        }

        return true;
    }

    private void scanRegionFiles(org.bukkit.World world, ItemStack targetItem, ScanResult result, Consumer<String> progressCallback) {
        File regionDir = new File(world.getWorldFolder(), "region");
        if (!regionDir.exists()) return;

        File[] regionFiles = regionDir.listFiles((d, name) -> name.endsWith(".mca"));
        if (regionFiles == null || regionFiles.length == 0) return;

        progressCallback.accept("§7Lecture de §e" + regionFiles.length + "§7 fichiers région dans §a" + world.getName() + "§7...");

        String targetMaterial = targetItem.getType().name();
        String targetIAId = (itemsAdderService != null && itemsAdderService.isEnabled())
                ? itemsAdderService.getItemId(targetItem) : null;

        int found = 0;
        for (File regionFile : regionFiles) {
            try {
                found += scanRegionFile(regionFile, world, targetMaterial, targetIAId, result);
            } catch (Exception e) {}
        }
        progressCallback.accept("§e" + found + "§7 items trouvés dans les fichiers région de §a" + world.getName());
    }

    private int scanRegionFile(File regionFile, org.bukkit.World world, String targetMaterial, String targetIAId, ScanResult result) throws Exception {
        int totalFound = 0;
        String[] parts = regionFile.getName().replace(".mca", "").split("\\.");
        int regionX = Integer.parseInt(parts[1]);
        int regionZ = Integer.parseInt(parts[2]);

        try (RandomAccessFile raf = new RandomAccessFile(regionFile, "r")) {
            if (raf.length() < 8192) return 0;

            for (int localX = 0; localX < 32; localX++) {
                for (int localZ = 0; localZ < 32; localZ++) {
                    int chunkX = regionX * 32 + localX;
                    int chunkZ = regionZ * 32 + localZ;

                    if (world.isChunkLoaded(chunkX, chunkZ)) continue;

                    int headerIndex = (localX + localZ * 32) * 4;
                    raf.seek(headerIndex);
                    int offset = ((raf.read() << 16) | (raf.read() << 8) | raf.read()) * 4096;
                    int sectorCount = raf.read();
                    if (offset == 0 || sectorCount == 0) continue;

                    raf.seek(offset);
                    int length = raf.readInt();
                    int compression = raf.read();
                    if (length <= 1) continue;

                    byte[] compressed = new byte[length - 1];
                    raf.readFully(compressed);

                    InputStream is = (compression == 2)
                            ? new java.util.zip.InflaterInputStream(new ByteArrayInputStream(compressed))
                            : new java.util.zip.GZIPInputStream(new ByteArrayInputStream(compressed));

                    totalFound += parseChunkNBT(is, chunkX, chunkZ, world, targetMaterial, targetIAId, result);
                }
            }
        }
        return totalFound;
    }

    private int parseChunkNBT(InputStream rawIs, int chunkX, int chunkZ, org.bukkit.World world,
                              String targetMaterial, String targetIAId, ScanResult result) {
        int found = 0;
        try {
            var dataInput = new java.io.DataInputStream(rawIs);

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

            if (chunkNBT == null) return 0;

            var getListMethod = chunkNBT.getClass().getMethod("getList", String.class, int.class);
            var blockEntities = getListMethod.invoke(chunkNBT, "block_entities", 10);
            if (blockEntities == null) return 0;

            var sizeMethod = blockEntities.getClass().getMethod("size");
            var getCompoundMethod = blockEntities.getClass().getMethod("getCompound", int.class);
            int size = (int) sizeMethod.invoke(blockEntities);

            for (int i = 0; i < size; i++) {
                var be = getCompoundMethod.invoke(blockEntities, i);
                var getStringMethod = be.getClass().getMethod("getString", String.class);
                String beId = (String) getStringMethod.invoke(be, "id");
                if (!isContainerType(beId)) continue;

                var getIntMethod = be.getClass().getMethod("getInt", String.class);
                int beX = (int) getIntMethod.invoke(be, "x");
                int beY = (int) getIntMethod.invoke(be, "y");
                int beZ = (int) getIntMethod.invoke(be, "z");

                var containsMethod = be.getClass().getMethod("contains", String.class);
                if (!(boolean) containsMethod.invoke(be, "Items")) continue;

                var items = getListMethod.invoke(be, "Items", 10);
                int count = countNBTItemsReflect(items, targetMaterial, targetIAId);
                if (count > 0) {
                    result.addLocation(new Location(world, beX, beY, beZ), count);
                    found += count;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ScanManager] Erreur NBT chunk " + chunkX + "," + chunkZ + " : " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
        return found;
    }

    private int countNBTItemsReflect(Object items, String targetMaterial, String targetIAId) {
        int count = 0;
        try {
            var sizeMethod = items.getClass().getMethod("size");
            var getCompoundMethod = items.getClass().getMethod("getCompound", int.class);
            int size = (int) sizeMethod.invoke(items);

            for (int i = 0; i < size; i++) {
                var item = getCompoundMethod.invoke(items, i);
                var getStringMethod = item.getClass().getMethod("getString", String.class);
                var containsMethod = item.getClass().getMethod("contains", String.class);

                String id = ((String) getStringMethod.invoke(item, "id"))
                        .replace("minecraft:", "").toUpperCase();

                int amount = 1;
                var getByteMethod = item.getClass().getMethod("getByte", String.class);
                var getIntMethod = item.getClass().getMethod("getInt", String.class);
                if ((boolean) containsMethod.invoke(item, "count")) {
                    amount = (int) getIntMethod.invoke(item, "count");
                } else if ((boolean) containsMethod.invoke(item, "Count")) {
                    amount = (byte) getByteMethod.invoke(item, "Count");
                }

                if (targetIAId != null) {
                    String iaId = extractItemsAdderIdReflect(item, containsMethod, getStringMethod);
                    if (targetIAId.equals(iaId)) count += amount;
                } else {
                    if (id.equals(targetMaterial)) count += amount;
                }
            }
        } catch (Exception ignored) {}
        return count;
    }

    private String extractItemsAdderIdReflect(Object item, java.lang.reflect.Method containsMethod, java.lang.reflect.Method getStringMethod) {
        try {
            var getCompoundMethod = item.getClass().getMethod("getCompound", String.class);

            if ((boolean) containsMethod.invoke(item, "components")) {
                var components = getCompoundMethod.invoke(item, "components");
                var compContains = components.getClass().getMethod("contains", String.class);
                var compGetCompound = components.getClass().getMethod("getCompound", String.class);
                if ((boolean) compContains.invoke(components, "minecraft:custom_data")) {
                    var customData = compGetCompound.invoke(components, "minecraft:custom_data");
                    var cdContains = customData.getClass().getMethod("contains", String.class);
                    var cdGetCompound = customData.getClass().getMethod("getCompound", String.class);
                    if ((boolean) cdContains.invoke(customData, "itemsadder")) {
                        var iaTag = cdGetCompound.invoke(customData, "itemsadder");
                        return (String) iaTag.getClass().getMethod("getString", String.class).invoke(iaTag, "id");
                    }
                }
            }
            if ((boolean) containsMethod.invoke(item, "tag")) {
                var tag = getCompoundMethod.invoke(item, "tag");
                var tagContains = tag.getClass().getMethod("contains", String.class);
                var tagGetCompound = tag.getClass().getMethod("getCompound", String.class);
                if ((boolean) tagContains.invoke(tag, "itemsadder")) {
                    var iaTag = tagGetCompound.invoke(tag, "itemsadder");
                    return (String) iaTag.getClass().getMethod("getString", String.class).invoke(iaTag, "id");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isContainerType(String id) {
        return id.contains("chest") || id.contains("barrel") || id.contains("hopper")
                || id.contains("dispenser") || id.contains("dropper") || id.contains("furnace")
                || id.contains("blast_furnace") || id.contains("smoker") || id.contains("shulker_box")
                || id.contains("trapped_chest");
    }
}
