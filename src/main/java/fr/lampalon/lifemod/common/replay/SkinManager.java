package fr.lampalon.lifemod.common.replay;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player skins for NPC injection in replays.
 * Stores skin data (textures/signatures) to ensure NPCs look like the recorded player.
 */
public class SkinManager {

    private final Map<UUID, TextureProperty[]> skinCache = new ConcurrentHashMap<>();

    /**
     * Caches skin properties for a player.
     * @param uuid The player's UUID.
     * @param properties Texture properties (textures and signatures).
     */
    public void cacheSkin(UUID uuid, TextureProperty[] properties) {
        skinCache.put(uuid, properties);
    }

    /**
     * Gets cached skin properties for a player.
     * @param uuid The player's UUID.
     * @return Cached properties, or null if not found.
     */
    public TextureProperty[] getSkin(UUID uuid) {
        return skinCache.get(uuid);
    }
}
