package fr.lampalon.lifemod.platform.bukkit.replay;

import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

public class ReplayPositionRecorder extends BukkitRunnable {

    private final ReplayManager replayManager;

    public ReplayPositionRecorder(LifeMod plugin) {
        this.replayManager = plugin.getReplayManager();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (replayManager.isRecording(uuid)) {
                ReplaySession session = replayManager.getSession(uuid);
                if (session != null) {
                    Location loc = player.getLocation();
                    
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        
                        // Magic byte 0xFE indicates a custom internal position packet
                        dos.writeByte(0xFE);
                        dos.writeDouble(loc.getX());
                        dos.writeDouble(loc.getY());
                        dos.writeDouble(loc.getZ());
                        dos.writeFloat(loc.getYaw());
                        dos.writeFloat(loc.getPitch());
                        dos.writeBoolean(player.isOnGround());
                        
                        byte[] data = baos.toByteArray();
                        
                        // Add it to the replay buffer
                        session.addFrame(new ReplayFrame(System.currentTimeMillis(), Collections.singletonList(data)));
                        
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
