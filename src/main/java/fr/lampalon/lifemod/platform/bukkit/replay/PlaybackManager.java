package fr.lampalon.lifemod.platform.bukkit.replay;

import fr.lampalon.lifemod.common.replay.EntityIdMapper;
import fr.lampalon.lifemod.common.replay.SkinManager;
import fr.lampalon.lifemod.common.replay.Interpolator;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;

import java.util.List;

/**
 * Manages the playback of recorded replay data using dedicated replay tools.
 */
public class PlaybackManager {

    private final LifeMod plugin;
    private final EntityIdMapper idMapper;
    private final SkinManager skinManager;
    private List<ReplayFrame> frames;
    private int currentIndex = 0;

    public PlaybackManager(LifeMod plugin) {
        this.plugin = plugin;
        this.idMapper = new EntityIdMapper();
        this.skinManager = new SkinManager();
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
                    idMapper.clear();
                    return;
                }

                ReplayFrame frame = frames.get(currentIndex);
                // Here we would apply interpolation for smooth movement:
                // Location interpolated = Interpolator.interpolateLocation(lastLoc, currentLoc, 0.5);
                
                // NPC spawning/updating would be handled here via NMSReplayHandler
                
                currentIndex++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
