package fr.lampalon.lifemod.platform.bukkit.replay;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ReplayPositionRecorder extends BukkitRunnable {

    private static final Logger LOGGER = Logger.getLogger("ReplayPositionRecorder");

    private static final double MOVE_THRESHOLD = 0.05;
    private static final float ROTATE_THRESHOLD = 3.0f;

    private final ReplayManager replayManager;
    private final Map<UUID, double[]> lastStates = new ConcurrentHashMap<>();

    public ReplayPositionRecorder(LifeMod plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        this.replayManager = plugin.getReplayManager();
        if (this.replayManager == null) {
            throw new IllegalStateException("ReplayManager not available from plugin");
        }
    }

    @Override
    public void run() {
        lastStates.keySet().removeIf(uuid -> !replayManager.isRecording(uuid));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) continue;
            UUID uuid = player.getUniqueId();
            if (!replayManager.isRecording(uuid)) continue;
            ReplaySession session = replayManager.getSession(uuid);
            if (session == null) continue;

            try {
                Location loc = player.getLocation();
                if (loc == null) continue;

                double[] last = lastStates.get(uuid);
                boolean moved = true;
                if (last != null) {
                    double dx = loc.getX() - last[0];
                    double dy = loc.getY() - last[1];
                    double dz = loc.getZ() - last[2];
                    float dyaw = Math.abs(loc.getYaw() - (float) last[3]);
                    float dpitch = Math.abs(loc.getPitch() - (float) last[4]);
                    if ((dx * dx + dy * dy + dz * dz) < MOVE_THRESHOLD * MOVE_THRESHOLD
                            && dyaw < ROTATE_THRESHOLD && dpitch < ROTATE_THRESHOLD) {
                        moved = false;
                    }
                }

                if (moved) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(34);
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeByte(ReplayCodec.POSITION);
                    dos.writeDouble(loc.getX());
                    dos.writeDouble(loc.getY());
                    dos.writeDouble(loc.getZ());
                    dos.writeFloat(loc.getYaw());
                    dos.writeFloat(loc.getPitch());
                    dos.writeBoolean(player.isOnGround());

                    session.queuePacket(baos.toByteArray());
                    lastStates.put(uuid, new double[]{loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()});
                }

                scanNearbyPlayers(session, player);
                session.flushFrame();
            } catch (Exception e) {
                LOGGER.warning("ReplayPositionRecorder error for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private void scanNearbyPlayers(ReplaySession session, Player recorder) {
        if (!recordNearbyPlayers()) return;
        double radius = recordRadius();
        double r2 = radius * radius;

        Set<Integer> seen = new HashSet<>();
        for (Player other : recorder.getWorld().getPlayers()) {
            if (other.getUniqueId().equals(recorder.getUniqueId())) continue;
            Location ol = other.getLocation();
            if (ol.distanceSquared(recorder.getLocation()) > r2) continue;

            int eId = other.getEntityId();
            seen.add(eId);
            if (session.isRecordable(eId)) continue;

            byte[] frame = ReplayCodec.buildSpawnPlayerFrame(eId, other.getUniqueId(), other.getName(),
                    ol.getX(), ol.getY(), ol.getZ(), ol.getYaw(), ol.getPitch());
            if (frame == null) continue;
            session.trackSpawn(eId);
            session.trackRecordablePlayer(eId);
            session.queuePacket(frame);
        }

        for (int eId : new HashSet<>(session.getRecordablePlayers())) {
            if (seen.contains(eId)) continue;
            session.untrackRecordablePlayer(eId);
            session.trackDestroy(eId);
            byte[] df = ReplayCodec.buildDestroyFrame(eId);
            if (df != null) session.queuePacket(df);
        }
    }

    private double recordRadius() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        return cfg != null ? cfg.getDouble("modules.replay.record-radius", 300.0) : 300.0;
    }

    private boolean recordNearbyPlayers() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        return cfg != null ? cfg.getBoolean("modules.replay.record-nearby-players", true) : true;
    }
}
