package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.adapter.IItemsAdderService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.model.ScanResult;
import org.bukkit.*;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ScanManager {
    private final LifeMod plugin;
    private final ScanConfig scanConfig;
    private final IItemsAdderService itemsAdderService;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    // Oraxen reflection cache
    private boolean oraxenChecked = false;
    private Method oraxenGetIdByItem;
    private Method oraxenGetItemById;

    public ScanManager(LifeMod plugin) {
        this.plugin = plugin;
        this.scanConfig = new ScanConfig(ServiceRegistry.get(IConfigurationService.class));
        this.itemsAdderService = ServiceRegistry.get(IItemsAdderService.class);
        initOraxen();
    }

    private void initOraxen() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Oraxen") != null) {
                Class<?> oraxenItems = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                oraxenGetIdByItem = oraxenItems.getMethod("getIdByItem", ItemStack.class);
                oraxenGetItemById = oraxenItems.getMethod("getItemById", String.class);
                plugin.getLogger().info("[ScanManager] Oraxen integration enabled");
            }
        } catch (Exception e) {
            plugin.getLogger().fine("[ScanManager] Oraxen not available: " + e.getMessage());
        }
        oraxenChecked = true;
    }

    private String getOraxenId(ItemStack item) {
        if (!oraxenChecked || oraxenGetIdByItem == null || item == null) return null;
        try {
            Object result = oraxenGetIdByItem.invoke(null, item);
            return result instanceof String s ? s : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack getOraxenItem(String id) {
        if (!oraxenChecked || oraxenGetItemById == null || id == null) return null;
        try {
            Object result = oraxenGetItemById.invoke(null, id);
            return result instanceof ItemStack is ? is : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void resetCancel() {
        cancelled.set(false);
    }

    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    private boolean reachedLimit(ScanResult result) {
        int limit = scanConfig.getMaxLocations();
        return limit > 0 && result.getTotalCount() >= limit;
    }

    /**
     * Scans for a specific item based on its material and custom data.
     */
    public CompletableFuture<ScanResult> scan(String type, String target, ItemStack targetItem, Consumer<String> progressCallback) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        resetCancel();
        CompletableFuture<ScanResult> future = CompletableFuture.supplyAsync(() -> {
            ScanResult result = new ScanResult();
            try {
                progressCallback.accept(lang.getMessage("commands.scan.progress.starting", "%type%", type));

                if (cancelled.get()) return result;

                if (type.equalsIgnoreCase("inventories") || type.equalsIgnoreCase("all")) {
                    scanInventories(target, targetItem, result, progressCallback);
                }

                if (cancelled.get()) return result;

                if (type.equalsIgnoreCase("enderchest") || type.equalsIgnoreCase("all")) {
                    scanEnderchests(target, targetItem, result, progressCallback);
                }

                if (cancelled.get()) return result;

                if (type.equalsIgnoreCase("map") || type.equalsIgnoreCase("all")) {
                    scanMap(targetItem, result, progressCallback);
                }

                return result;
            } finally {
                result.complete();
            }
        });
        int timeout = scanConfig.getTimeoutSeconds();
        if (timeout > 0) {
            future = future.orTimeout(timeout, TimeUnit.SECONDS);
        }
        return future;
    }

    private void scanMap(ItemStack targetItem, ScanResult result, Consumer<String> progressCallback) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        NMSProvider nmsProvider = ServiceRegistry.get(ILifePlatform.class).getNmsProvider();
        if (nmsProvider == null) {
            progressCallback.accept(lang.getMessage("commands.scan.progress.error", "%message%", "NMS not available"));
            return;
        }

        // Collecte des chunks DÉJÀ chargés sur le main thread (safe, rapide)
        CompletableFuture<Map<World, List<Container>>> loadedFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Map<World, List<Container>> map = new HashMap<>();
            for (World world : Bukkit.getWorlds()) {
                map.put(world, nmsProvider.getLoadedContainers(world));
            }
            loadedFuture.complete(map);
        });

        try {
            Map<World, List<Container>> loadedContainers = loadedFuture.get(10, TimeUnit.SECONDS);

            // 1. Scan des chunks chargés (inventaire live)
            for (var entry : loadedContainers.entrySet()) {
                progressCallback.accept(lang.getMessage("commands.scan.progress.containers", "%count%", String.valueOf(entry.getValue().size()), "%world%", entry.getKey().getName()));
                for (Container container : entry.getValue()) {
                    int count = countItems(container.getInventory().getContents(), targetItem);
                    if (count > 0) result.addLocation(container.getLocation(), count);
                    if (reachedLimit(result)) {
                        progressCallback.accept(lang.getMessage("commands.scan.progress.error", "%message%", "Hit max-locations limit"));
                        return;
                    }
                }
                if (cancelled.get()) return;
            }

            // 2. Scan des fichiers région (chunks non chargés) — ASYNC, pas de freeze
            if (scanConfig.isRegionFileScan()) {
                for (World world : Bukkit.getWorlds()) {
                    if (cancelled.get()) return;
                    scanRegionFiles(world, targetItem, result, progressCallback);
                }
            }

        } catch (Exception e) {
            progressCallback.accept(lang.getMessage("commands.scan.progress.error", "%message%", e.getMessage()));
        }
    }

    private void scanInventories(String target, ItemStack targetItem, ScanResult result, Consumer<String> progressCallback) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        if (target.equalsIgnoreCase("all")) {
            progressCallback.accept(lang.getMessage("commands.scan.progress.inventories-all"));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (cancelled.get() || reachedLimit(result)) return;
                int count = countItems(player.getInventory().getContents(), targetItem);
                if (count > 0) result.addPlayer(player.getUniqueId(), count);
            }
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                progressCallback.accept(lang.getMessage("commands.scan.progress.inventory-player", "%player%", player.getName()));
                int count = countItems(player.getInventory().getContents(), targetItem);
                if (count > 0) result.addPlayer(player.getUniqueId(), count);
            }
        }
    }

    private void scanEnderchests(String target, ItemStack targetItem, ScanResult result, Consumer<String> progressCallback) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        if (target.equalsIgnoreCase("all")) {
            progressCallback.accept(lang.getMessage("commands.scan.progress.enderchests-all"));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (cancelled.get() || reachedLimit(result)) return;
                int count = countItems(player.getEnderChest().getContents(), targetItem);
                if (count > 0) result.addPlayer(player.getUniqueId(), count);
            }
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                progressCallback.accept(lang.getMessage("commands.scan.progress.enderchest-player", "%player%", player.getName()));
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
     * Supports: Materials, CustomModelData, ItemsAdder, Oraxen, full NBT (isSimilar).
     */
    public boolean isMatch(ItemStack item, ItemStack target) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (target == null || target.getType() == Material.AIR) return false;

        // 1. ItemsAdder match
        if (itemsAdderService != null && itemsAdderService.isEnabled()) {
            String itemId = itemsAdderService.getItemId(item);
            String targetId = itemsAdderService.getItemId(target);
            if (itemId != null || targetId != null) {
                return itemId != null && itemId.equals(targetId);
            }
        }

        // 2. Oraxen match
        String oraxenItemId = getOraxenId(item);
        String oraxenTargetId = getOraxenId(target);
        if (oraxenItemId != null || oraxenTargetId != null) {
            return oraxenItemId != null && oraxenItemId.equals(oraxenTargetId);
        }

        // 3. Material mismatch
        if (item.getType() != target.getType()) return false;

        // 4. Strict matching via ItemStack.isSimilar (compares full item meta + data)
        if (scanConfig.isStrictItemMatching()) {
            if (item.hasItemMeta() != target.hasItemMeta()) return false;
            if (item.hasItemMeta() && !Bukkit.getItemFactory().equals(item.getItemMeta(), target.getItemMeta())) return false;
        } else {
            // Legacy: compare only CustomModelData
            if (item.hasItemMeta() && target.hasItemMeta()) {
                if (item.getItemMeta().hasCustomModelData() != target.getItemMeta().hasCustomModelData()) return false;
                if (item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() != target.getItemMeta().getCustomModelData()) return false;
            }
        }

        return true;
    }

    private void scanRegionFiles(World world, ItemStack targetItem, ScanResult result, Consumer<String> progressCallback) {
        NMSProvider nmsProvider = ServiceRegistry.get(ILifePlatform.class).getNmsProvider();
        if (nmsProvider == null) return;
        ILangService lang = ServiceRegistry.get(ILangService.class);
        File regionDir = new File(world.getWorldFolder(), "region");
        if (!regionDir.exists()) return;

        File[] regionFiles = regionDir.listFiles((d, name) -> name.endsWith(".mca"));
        if (regionFiles == null || regionFiles.length == 0) return;

        progressCallback.accept(lang.getMessage("commands.scan.progress.region-files", "%files%", String.valueOf(regionFiles.length), "%world%", world.getName()));

        String targetMaterial = targetItem.getType().name();
        String targetIAId = (itemsAdderService != null && itemsAdderService.isEnabled())
                ? itemsAdderService.getItemId(targetItem) : null;

        int maxFiles = scanConfig.getMaxRegionFiles();
        if (maxFiles > 0 && regionFiles.length > maxFiles) {
            regionFiles = Arrays.copyOf(regionFiles, maxFiles);
        }

        int found = Arrays.stream(regionFiles)
                .takeWhile(f -> !cancelled.get())
                .mapToInt(file -> {
                    try {
                        return scanRegionFile(nmsProvider, file, world, targetMaterial, targetIAId, result);
                    } catch (Exception e) {
                        plugin.getLogger().warning("[ScanManager] Error reading " + file.getName() + ": " + e.getMessage());
                        return 0;
                    }
                })
                .sum();
        progressCallback.accept(lang.getMessage("commands.scan.progress.region-found", "%found%", String.valueOf(found), "%world%", world.getName()));
    }

    private int scanRegionFile(NMSProvider nms, File regionFile, org.bukkit.World world,
                               String targetMaterial, String targetIAId, ScanResult result) throws Exception {
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

                    List<NMSProvider.ChunkItemHit> hits = nms.scanChunkItems(is, targetMaterial, targetIAId);
                    for (NMSProvider.ChunkItemHit hit : hits) {
                        result.addLocation(new Location(world, hit.x, hit.y, hit.z), hit.count);
                        totalFound += hit.count;
                    }
                }
            }
        }
        return totalFound;
    }
}
