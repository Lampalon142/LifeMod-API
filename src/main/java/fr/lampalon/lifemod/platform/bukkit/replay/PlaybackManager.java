package fr.lampalon.lifemod.platform.bukkit.replay;

import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;

import java.util.List;

/**
 * Manages the playback of recorded replay data.
 */
public class PlaybackManager {

    private final LifeMod plugin;
    private List<ReplayFrame> frames;
    private int currentIndex = 0;

    public PlaybackManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts replaying captured frames to a spectator.
     * @param spectator The mod viewing the replay.
     * @param frames The frames to play.
     */
    public void startPlayback(Player spectator, List<ReplayFrame> frames) {
        this.frames = frames;
        this.currentIndex = 0;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentIndex >= frames.size()) {
                    this.cancel();
                    return;
                }

                ReplayFrame frame = frames.get(currentIndex);
                // Here we inject the packets to the spectator using PacketEvents
                // Example: PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, frame.getPackets());
                
                currentIndex++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Playback speed: 1 frame per tick
    }
}
