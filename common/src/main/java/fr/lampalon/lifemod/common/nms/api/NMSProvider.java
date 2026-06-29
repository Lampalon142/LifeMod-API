// NMS abstraction boundary - Bukkit types are acceptable here
package fr.lampalon.lifemod.common.nms.api;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface NMSProvider {

    class ChunkItemHit {
        public final int x, y, z;
        public final int count;
        public ChunkItemHit(int x, int y, int z, int count) {
            this.x = x; this.y = y; this.z = z; this.count = count;
        }
    }

    void sendActionBar(Player player, String message);

    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void kickPlayer(Player player, String reason);

    int getPing(Player player);

    String getName();

    List<Container> getLoadedContainers(World world);

    List<ChunkItemHit> scanChunkItems(InputStream chunkData, String targetMaterial, String targetIAId);

    default void spawnNPC(Player spectator, int entityId, UUID uuid, String name,
                          TextureProperty[] skin, Location location) {
    }

    default void removeNPC(Player spectator, int entityId, UUID uuid) {
    }
}
