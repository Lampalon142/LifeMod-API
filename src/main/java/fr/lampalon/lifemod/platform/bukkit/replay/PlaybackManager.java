package fr.lampalon.lifemod.platform.bukkit.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import fr.lampalon.lifemod.common.nms.api.NMSReplayHandler;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the playback of recorded replay data.
 */
public class PlaybackManager {
    private static final Logger LOGGER = Logger.getLogger("PlaybackManager");
    private final LifeMod plugin;
    private List<ReplayFrame> frames;
    private int currentIndex = 0;
    private UUID npcUUID;

    public PlaybackManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    public void startPlayback(Player spectator, List<ReplayFrame> frames, ReplaySession session) {
        this.frames = frames;
        this.currentIndex = 0;
        this.npcUUID = UUID.randomUUID();
        
        int originalEntityId = session.getEntityId();
        String playerName = session.getPlayerName();
        UUID playerUUID = session.getPlayerUUID();

        LOGGER.info("[DEBUG] Starting playback for " + spectator.getName() + " (Target: " + playerName + ")");
        LOGGER.info("[DEBUG] Frames: " + frames.size() + ", Original Entity ID: " + originalEntityId);

        NMSReplayHandler nms = (NMSReplayHandler) ServiceRegistry.get(ILifePlatform.class).getNmsProvider();
        
        // Get skin
        TextureProperty[] skin = plugin.getReplayManager().getSkinManager().getSkin(playerUUID);
        
        // Use the recorded start position
        Location spawnLoc = new Location(spectator.getWorld(), session.getStartX(), session.getStartY(), session.getStartZ(), session.getStartYaw(), session.getStartPitch());
        
        // Spawn NPC with the original entity ID so the recorded packets apply to it!
        nms.spawnNPC(spectator, originalEntityId, npcUUID, playerName, skin, spawnLoc);
        
        // Hide the real player from the spectator
        Player realPlayer = Bukkit.getPlayer(playerUUID);
        if (realPlayer != null && realPlayer.isOnline()) {
            spectator.hidePlayer(plugin, realPlayer);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentIndex >= frames.size()) {
                    LOGGER.info("[DEBUG] Replay finished for " + spectator.getName());
                    nms.removeNPC(spectator, npcUUID);
                    if (realPlayer != null && realPlayer.isOnline()) {
                        spectator.showPlayer(plugin, realPlayer);
                    }
                    this.cancel();
                    return;
                }

                ReplayFrame frame = frames.get(currentIndex);
                for (byte[] data : frame.getPackets()) {
                    try {
                        // Send raw packet to spectator using Netty Unpooled buffer
                        PacketEvents.getAPI().getProtocolManager().sendPacket(spectator, 
                            io.netty.buffer.Unpooled.wrappedBuffer(data));
                    } catch (Exception e) {
                        // Ignore individual packet errors
                    }
                }
                
                currentIndex++;
                
                if (currentIndex % 200 == 0) {
                    spectator.sendMessage("§7Replay progress: " + (int)((currentIndex/(double)frames.size())*100) + "%");
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
