package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import fr.lampalon.lifemod.common.replay.SkinManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.logging.Logger;

/**
 * Caches player skins when they join the server.
 *
 * Essential for offline/cracked servers where Mojang API returns 204
 * for offline UUIDs. PacketEvents stores the real skin (from auth or
 * skin plugin like SkinsRestorer) in the player's UserProfile.
 */
public class ReplayJoinListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger("ReplayJoinListener");
    private final SkinManager skinManager;

    public ReplayJoinListener(SkinManager skinManager) {
        this.skinManager = skinManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();

        // PacketEvents stores the authenticated profile including skin textures
        UserProfile profile = PacketEvents.getAPI()
                .getPlayerManager()
                .getUser(player)
                .getProfile();

        if (profile == null) return;

        List<TextureProperty> props = profile.getTextureProperties();
        if (props == null || props.isEmpty()) {
            LOGGER.info("[ReplayJoinListener] No skin for " + player.getName()
                    + " (offline/no skin plugin)");
            return;
        }

        TextureProperty[] arr = props.toArray(new TextureProperty[0]);

        // Cache by UUID and by name (name cache is the fallback for offline servers)
        skinManager.cacheSkin(player.getUniqueId(), arr);
        skinManager.cacheSkinByName(player.getName(), arr);

        LOGGER.info("[ReplayJoinListener] Cached skin for " + player.getName()
                + " (" + props.size() + " properties)");
    }
}