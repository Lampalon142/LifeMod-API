package fr.lampalon.lifemod.platform.bukkit.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import fr.lampalon.lifemod.common.replay.SkinManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Resolves NPC skin textures without blocking the server thread.
 *
 * Priority: cached textures -> live player's PacketEvents profile (works for
 * online-mode accounts and SkinsRestorer-style custom skins) -> Mojang fetch
 * (only call from an async context via {@link SkinManager#getOrFetchSkin}).
 */
public final class SkinUtil {

    private SkinUtil() {
    }

    public static TextureProperty[] resolveSkin(SkinManager mgr, UUID uuid, String name) {
        TextureProperty[] cached = mgr.getSkinOrByName(uuid, name);
        if (cached != null && cached.length > 0) return cached;

        Player p = uuid != null ? Bukkit.getPlayer(uuid) : null;
        if (p == null && name != null) p = Bukkit.getPlayerExact(name);
        if (p != null && p.isOnline()) {
            try {
                UserProfile profile = PacketEvents.getAPI()
                        .getPlayerManager().getUser(p).getProfile();
                if (profile != null && profile.getTextureProperties() != null
                        && !profile.getTextureProperties().isEmpty()) {
                    TextureProperty[] arr = profile.getTextureProperties()
                            .toArray(new TextureProperty[0]);
                    mgr.cacheSkin(uuid, arr);
                    mgr.cacheSkinByName(name, arr);
                    return arr;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
