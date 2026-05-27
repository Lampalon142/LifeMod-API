package fr.lampalon.lifemod.common.replay;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages player skins for NPC injection in replays.
 *
 * For offline/cracked servers, skins must be cached when the player joins
 * (via ReplayAutoStartListener or a dedicated join listener) because
 * the Mojang API won't recognize offline-mode UUIDs.
 *
 * For online servers, falls back to the Mojang session API if not cached.
 */
public class SkinManager {

    private static final Logger LOGGER = Logger.getLogger("SkinManager");

    private final Map<UUID, TextureProperty[]> skinCache = new ConcurrentHashMap<>();
    private final Map<String, TextureProperty[]> skinCacheByName = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "SkinManager-Cleanup");
        t.setDaemon(true);
        return t;
    });

    public SkinManager() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 30, 30, TimeUnit.MINUTES);
    }

    private void cleanup() {
        if (skinCache.size() > 1000) {
            skinCache.clear();
        }
        if (skinCacheByName.size() > 1000) {
            skinCacheByName.clear();
        }
    }

    public void cacheSkin(UUID uuid, TextureProperty[] properties) {
        if (properties != null && properties.length > 0) {
            skinCache.put(uuid, properties);
        }
    }

    public void cacheSkinByName(String name, TextureProperty[] properties) {
        if (properties != null && properties.length > 0) {
            skinCacheByName.put(name.toLowerCase(), properties);
        }
    }

    public TextureProperty[] getSkin(UUID uuid) {
        return skinCache.get(uuid);
    }

    public TextureProperty[] getSkinByName(String name) {
        return skinCacheByName.get(name.toLowerCase());
    }

    /**
     * Returns cached skin by UUID. If not found, tries by name.
     * For offline servers, the UUID cache may be empty but name cache works.
     */
    public TextureProperty[] getSkinOrByName(UUID uuid, String name) {
        TextureProperty[] skin = skinCache.get(uuid);
        if (skin != null && skin.length > 0) return skin;
        if (name != null) return skinCacheByName.get(name.toLowerCase());
        return null;
    }

    /**
     * For ONLINE mode servers only.
     * Fetches skin from Mojang session API if not cached.
     * Do NOT call on the main thread.
     */
    public TextureProperty[] getOrFetchSkin(UUID uuid) {
        TextureProperty[] cached = skinCache.get(uuid);
        if (cached != null && cached.length > 0) return cached;

        LOGGER.info("[SkinManager] Skin not cached for " + uuid + ", fetching from Mojang...");
        try {
            String uuidNoDashes = uuid.toString().replace("-", "");
            String sessionUrl = "https://sessionserver.mojang.com/session/minecraft/profile/"
                    + uuidNoDashes + "?unsigned=false";

            HttpURLConnection conn = (HttpURLConnection) new URL(sessionUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code != 200) {
                LOGGER.warning("[SkinManager] Mojang API returned " + code + " for " + uuid
                        + " (offline server? Use cacheSkin() on join instead)");
                return new TextureProperty[0];
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String json = response.toString();
            String textureValue = extractJsonString(json, "value");
            String signature    = extractJsonString(json, "signature");

            if (textureValue == null || textureValue.isEmpty()) {
                LOGGER.warning("[SkinManager] Could not parse texture for " + uuid);
                return new TextureProperty[0];
            }

            TextureProperty prop = new TextureProperty("textures", textureValue,
                    signature != null ? signature : "");
            TextureProperty[] result = {prop};
            skinCache.put(uuid, result);
            LOGGER.info("[SkinManager] Fetched and cached skin for " + uuid);
            return result;

        } catch (Exception e) {
            LOGGER.warning("[SkinManager] Fetch failed for " + uuid + ": " + e.getMessage());
            return new TextureProperty[0];
        }
    }

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