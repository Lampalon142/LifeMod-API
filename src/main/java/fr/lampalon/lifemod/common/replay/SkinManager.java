package fr.lampalon.lifemod.common.replay;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages player skins for NPC injection in replays.
 * Stores skin data (textures/signatures) to ensure NPCs look like the recorded player.
 * Falls back to the Mojang API if the skin is not cached locally.
 */
public class SkinManager {

    private static final Logger LOGGER = Logger.getLogger("SkinManager");

    private final Map<UUID, TextureProperty[]> skinCache = new ConcurrentHashMap<>();

    /**
     * Caches skin properties for a player.
     */
    public void cacheSkin(UUID uuid, TextureProperty[] properties) {
        if (properties != null && properties.length > 0) {
            skinCache.put(uuid, properties);
        }
    }

    /**
     * Gets cached skin properties for a player.
     * Returns null if not cached — use fetchAndCacheSkin() for a guaranteed result.
     */
    public TextureProperty[] getSkin(UUID uuid) {
        return skinCache.get(uuid);
    }

    /**
     * Returns the cached skin if available, otherwise fetches it from the Mojang API
     * synchronously and caches it.
     *
     * IMPORTANT: call this from an async thread (e.g. BukkitRunnable async),
     * never from the main thread, to avoid blocking the server.
     *
     * @param uuid Player UUID
     * @return TextureProperty array, or empty array if fetch failed
     */
    public TextureProperty[] getOrFetchSkin(UUID uuid) {
        TextureProperty[] cached = skinCache.get(uuid);
        if (cached != null && cached.length > 0) {
            return cached;
        }

        LOGGER.info("[SkinManager] Skin not cached for " + uuid + ", fetching from Mojang...");
        try {
            // Step 1: Get the session profile which includes the skin texture
            String uuidNoDashes = uuid.toString().replace("-", "");
            String sessionUrl = "https://sessionserver.mojang.com/session/minecraft/profile/"
                    + uuidNoDashes + "?unsigned=false";

            HttpURLConnection conn = (HttpURLConnection) new URL(sessionUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                LOGGER.warning("[SkinManager] Mojang API returned " + conn.getResponseCode()
                        + " for " + uuid);
                return new TextureProperty[0];
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();

            // Step 2: Parse the "properties" array manually (no external JSON lib needed)
            // Expected format: "properties":[{"name":"textures","value":"...","signature":"..."}]
            String textureValue = extractJsonString(json, "value");
            String signature    = extractJsonString(json, "signature");

            if (textureValue == null || textureValue.isEmpty()) {
                LOGGER.warning("[SkinManager] Could not parse texture value for " + uuid);
                return new TextureProperty[0];
            }

            TextureProperty prop = new TextureProperty("textures", textureValue,
                    signature != null ? signature : "");
            TextureProperty[] result = new TextureProperty[]{prop};
            skinCache.put(uuid, result);

            LOGGER.info("[SkinManager] Successfully fetched and cached skin for " + uuid);
            return result;

        } catch (Exception e) {
            LOGGER.warning("[SkinManager] Failed to fetch skin for " + uuid + ": " + e.getMessage());
            return new TextureProperty[0];
        }
    }

    /**
     * Extracts the first occurrence of a JSON string value by key.
     * Simple parser — works for flat Mojang API responses.
     */
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}