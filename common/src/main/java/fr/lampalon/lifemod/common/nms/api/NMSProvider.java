// NMS abstraction boundary - Bukkit types are acceptable here
package fr.lampalon.lifemod.common.nms.api;

import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import java.io.InputStream;
import java.util.List;

/**
 * Interface representing NMS (net.minecraft.server) capabilities.
 * This abstraction layer allows the plugin to be compatible with multiple Minecraft versions.
 * 
 * Règle : Aucune importation net.minecraft ou org.bukkit.craftbukkit ne doit figurer ici.
 */
public interface NMSProvider {

    class ChunkItemHit {
        public final int x, y, z;
        public final int count;
        public ChunkItemHit(int x, int y, int z, int count) {
            this.x = x; this.y = y; this.z = z; this.count = count;
        }
    }

    /**
     * Sends an action bar message to a player.
     *
     * @param player  The player receiving the message.
     * @param message The message to display.
     */
    void sendActionBar(Player player, String message);

    /**
     * Sends a title and subtitle to a player.
     *
     * @param player   The player receiving the title.
     * @param title    The title text.
     * @param subtitle The subtitle text.
     * @param fadeIn   The fade in time in ticks.
     * @param stay     The stay time in ticks.
     * @param fadeOut  The fade out time in ticks.
     */
    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * Kicks a player with a custom reason.
     *
     * @param player The player to kick.
     * @param reason The reason for the kick.
     */
    void kickPlayer(Player player, String reason);

    /**
     * Retrieves the ping of a player.
     *
     * @param player The player.
     * @return The ping in milliseconds.
     */
    int getPing(Player player);

    /**
     * Retrieves the name of this NMS version.
     *
     * @return The NMS version name.
     */
    String getName();

    /**
     * Efficiently retrieves all loaded container blocks in a world.
     *
     * @param world The world to scan.
     * @return A list of containers currently loaded.
     */
    List<Container> getLoadedContainers(World world);

    /**
     * Scans a compressed chunk NBT stream for container items matching the given criteria.
     *
     * @param chunkData      Decompressed chunk NBT input stream.
     * @param targetMaterial Material name to match (uppercase), or null if using IA id.
     * @param targetIAId     ItemsAdder item id to match, or null if using material.
     * @return List of hits with block coordinates and item counts.
     */
    List<ChunkItemHit> scanChunkItems(InputStream chunkData, String targetMaterial, String targetIAId);
}
