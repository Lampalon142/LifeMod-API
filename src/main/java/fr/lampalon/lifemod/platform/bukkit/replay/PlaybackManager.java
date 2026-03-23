package fr.lampalon.lifemod.platform.bukkit.replay;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import fr.lampalon.lifemod.common.nms.api.NMSReplayHandler;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Manages the playback of recorded replay data.
 *
 * Uses a guaranteed-unique virtual entity ID to avoid any conflict with the
 * moderator's own entity ID. Fetches the skin asynchronously from Mojang if
 * not already cached, before spawning the NPC.
 */
public class PlaybackManager {
    private static final Logger LOGGER = Logger.getLogger("PlaybackManager");

    private static final AtomicInteger VIRTUAL_ID_COUNTER = new AtomicInteger(900_000);

    private final LifeMod plugin;
    private List<ReplayFrame> frames;
    private int currentIndex = 0;
    private int virtualEntityId;
    private UUID npcUUID;
    private BukkitTask playbackTask;

    public PlaybackManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Public start methods
    // -------------------------------------------------------------------------

    public void startPlayback(Player spectator, List<ReplayFrame> frames, ReplaySession session) {
        startPlayback(spectator, frames,
                session.getEntityId(),
                session.getPlayerUUID(),
                session.getPlayerName(),
                new Location(spectator.getWorld(),
                        session.getStartX(), session.getStartY(), session.getStartZ(),
                        session.getStartYaw(), session.getStartPitch())
        );
    }

    public void startPlayback(Player spectator, List<ReplayFrame> frames,
                              int originalEntityId, UUID targetUUID,
                              String targetName, Location startLoc) {
        this.frames = frames;
        this.currentIndex = 0;
        this.npcUUID = UUID.randomUUID();
        this.virtualEntityId = VIRTUAL_ID_COUNTER.getAndIncrement();

        LOGGER.info("[DEBUG] Starting playback for " + spectator.getName()
                + " | Target: " + targetName
                + " | originalId: " + originalEntityId
                + " | virtualId: " + virtualEntityId
                + " | moderatorId: " + spectator.getEntityId());

        plugin.getReplayPlayerManager().registerPlayback(spectator, this);

        // Hide real player if online
        Player realPlayer = Bukkit.getPlayer(targetUUID);
        if (realPlayer != null && realPlayer.isOnline()) {
            spectator.hidePlayer(plugin, realPlayer);
        }

        // Teleport moderator behind the NPC start position
        spectator.teleport(buildObserverLocation(startLoc));

        // Fetch skin ASYNC (may call Mojang API) then spawn NPC on main thread
        new BukkitRunnable() {
            TextureProperty[] skin;

            @Override
            public void run() {
                // This runs async — safe to call Mojang API here
                skin = plugin.getReplayManager().getSkinManager().getOrFetchSkin(targetUUID);
                LOGGER.info("[DEBUG] Skin for " + targetName + ": "
                        + (skin == null || skin.length == 0 ? "NOT FOUND" : skin.length + " properties"));
            }

            // Runs on main thread after async task completes
            // We chain a sync task manually below
        }.runTaskAsynchronously(plugin);

        // We use a two-step approach: async fetch, then sync spawn
        // Chain: async fetch → sync spawn after 20 ticks (gives enough time for API response)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!spectator.isOnline()) return;

                TextureProperty[] skin =
                        plugin.getReplayManager().getSkinManager().getSkin(targetUUID);

                LOGGER.info("[DEBUG] Spawning NPC with skin: "
                        + (skin == null || skin.length == 0 ? "NULL/EMPTY — NPC may be invisible!" : "OK"));

                NMSReplayHandler nms = (NMSReplayHandler)
                        ServiceRegistry.get(ILifePlatform.class).getNmsProvider();

                nms.spawnNPC(spectator, virtualEntityId, npcUUID, targetName, skin, startLoc);
                startPlaybackLoop(spectator, realPlayer, originalEntityId);
            }
        }.runTaskLater(plugin, 20L); // 1 second — enough for the async skin fetch
    }

    // -------------------------------------------------------------------------
    // Stop
    // -------------------------------------------------------------------------

    public void stopPlayback() {
        if (playbackTask != null && !playbackTask.isCancelled()) {
            playbackTask.cancel();
            playbackTask = null;
        }
    }

    // -------------------------------------------------------------------------
    // Playback loop
    // -------------------------------------------------------------------------

    private void startPlaybackLoop(Player spectator, Player realPlayer, int originalEntityId) {
        playbackTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!spectator.isOnline() || !plugin.getReplayPlayerManager().isInReplay(spectator)) {
                    cleanup(spectator, realPlayer);
                    this.cancel();
                    return;
                }

                if (currentIndex >= frames.size()) {
                    LOGGER.info("[DEBUG] Replay finished for " + spectator.getName());
                    cleanup(spectator, realPlayer);
                    plugin.getReplayPlayerManager().exitReplay(spectator);
                    spectator.sendMessage("§aReplay success. Your state was been restored.");
                    this.cancel();
                    return;
                }

                ReplayFrame frame = frames.get(currentIndex);
                for (byte[] data : frame.getPackets()) {
                    try {
                        if (data.length > 0 && data[0] == (byte) 0xFE) {
                            // Custom position packet from ReplayPositionRecorder
                            DataInputStream dis =
                                    new DataInputStream(new ByteArrayInputStream(data));
                            dis.readByte(); // skip magic 0xFE
                            double  x        = dis.readDouble();
                            double  y        = dis.readDouble();
                            double  z        = dis.readDouble();
                            float   yaw      = dis.readFloat();
                            float   pitch    = dis.readFloat();
                            boolean onGround = dis.readBoolean();

                            WrapperPlayServerEntityTeleport teleport =
                                    new WrapperPlayServerEntityTeleport(
                                            virtualEntityId,
                                            new Vector3d(x, y, z),
                                            yaw, pitch, onGround);
                            PacketEvents.getAPI().getPlayerManager()
                                    .sendPacket(spectator, teleport);

                            WrapperPlayServerEntityHeadLook headLook =
                                    new WrapperPlayServerEntityHeadLook(virtualEntityId, yaw);
                            PacketEvents.getAPI().getPlayerManager()
                                    .sendPacket(spectator, headLook);

                        } else {
                            // Standard recorded packet — rewrite entity ID before sending
                            byte[] rewritten = rewriteEntityId(data, originalEntityId, virtualEntityId);
                            PacketEvents.getAPI().getProtocolManager().sendPacket(
                                    spectator,
                                    Unpooled.wrappedBuffer(rewritten));
                        }
                    } catch (Exception e) {
                        // Keep going even if one packet fails
                    }
                }

                currentIndex++;

                if (currentIndex % 200 == 0) {
                    int pct = (int) ((currentIndex / (double) frames.size()) * 100);
                    spectator.sendMessage("§7Replay: §e" + pct + "%");
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // -------------------------------------------------------------------------
    // Packet entity-ID rewriting
    // -------------------------------------------------------------------------

    private byte[] rewriteEntityId(byte[] data, int originalId, int virtualId) {
        try {
            ByteBuf buf = Unpooled.wrappedBuffer(data);

            readVarInt(buf); // skip packet ID
            int entityIdStartIdx = buf.readerIndex();
            int entityId = readVarInt(buf);
            int entityIdEndIdx = buf.readerIndex();

            if (entityId != originalId) return data;

            int oldLen = varIntSize(originalId);
            int newLen = varIntSize(virtualId);

            ByteBuf out = Unpooled.buffer(data.length - oldLen + newLen);
            buf.readerIndex(0);
            buf.readBytes(out, entityIdStartIdx);
            writeVarInt(out, virtualId);
            buf.readerIndex(entityIdEndIdx);
            buf.readBytes(out, buf.readableBytes());

            byte[] result = new byte[out.readableBytes()];
            out.readBytes(result);
            return result;

        } catch (Exception e) {
            return data;
        }
    }

    private int readVarInt(ByteBuf buf) {
        int value = 0, shift = 0;
        byte b;
        do {
            b = buf.readByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    private void writeVarInt(ByteBuf buf, int value) {
        while ((value & ~0x7F) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    private int varIntSize(int value) {
        int size = 0;
        do { value >>>= 7; size++; } while (value != 0);
        return size;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void cleanup(Player spectator, Player realPlayer) {
        NMSReplayHandler nms = (NMSReplayHandler)
                ServiceRegistry.get(ILifePlatform.class).getNmsProvider();
        nms.removeNPC(spectator, virtualEntityId, npcUUID);

        if (realPlayer != null && realPlayer.isOnline()) {
            spectator.showPlayer(plugin, realPlayer);
        }
    }

    private Location buildObserverLocation(Location npcLoc) {
        double yawRad = Math.toRadians(npcLoc.getYaw());
        double offsetX =  Math.sin(yawRad) * 5;
        double offsetZ = -Math.cos(yawRad) * 5;

        Location obs = npcLoc.clone();
        obs.setX(npcLoc.getX() + offsetX);
        obs.setY(npcLoc.getY() + 2);
        obs.setZ(npcLoc.getZ() + offsetZ);
        obs.setYaw(npcLoc.getYaw() + 180f);
        obs.setPitch(15f);
        return obs;
    }
}