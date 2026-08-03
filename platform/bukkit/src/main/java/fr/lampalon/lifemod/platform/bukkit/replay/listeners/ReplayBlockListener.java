package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.replay.ReplayCodec;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles block change recording for replays.
 *
 * Two things are recorded per block event:
 *
 * 1. BEFORE state (0xFA) — recorded at HIGHEST priority before the block changes.
 *    Used to restore the world to its original state at replay start.
 *
 * 2. AFTER state (0xFD = BlockChange packet) — recorded at MONITOR priority after
 *    the block has changed. This is what gets replayed to show the block appear/disappear.
 *    We build the packet manually here because BLOCK_CHANGE from PacketListener only
 *    fires for the player who caused it, not for the replay moderator.
 */
public class ReplayBlockListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger("ReplayBlockListener");
    private final ReplayManager replayManager;

    public ReplayBlockListener(ReplayManager replayManager) {
        if (replayManager == null) throw new IllegalArgumentException("replayManager cannot be null");
        this.replayManager = replayManager;
    }

    // ─── BREAK ───────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakBefore(BlockBreakEvent event) {
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;

        // Record BEFORE state (for undo at replay start)
        recordUndoFrame(session, event.getBlock());
        LifeMod.getInstance().getDebugManager().log("replay", "Break BEFORE at " + pos(event.getBlock())
                + " type=" + event.getBlock().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakAfter(BlockBreakEvent event) {
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;

        // Record AFTER state: block is now AIR (id=0)
        recordBlockChangeFrame(session,
                event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), 0);
        LifeMod.getInstance().getDebugManager().log("replay", "Break AFTER at " + pos(event.getBlock()) + " → AIR");
    }

    // ─── PLACE ───────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlaceBefore(BlockPlaceEvent event) {
        if (event instanceof BlockMultiPlaceEvent) return;
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;

        // Record BEFORE state (what was there before placement, usually AIR)
        recordUndoFrameFromState(session, event.getBlockReplacedState());
        LifeMod.getInstance().getDebugManager().log("replay", "Place BEFORE at " + pos(event.getBlockPlaced())
                + " replacing=" + event.getBlockReplacedState().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaceAfter(BlockPlaceEvent event) {
        if (event instanceof BlockMultiPlaceEvent) return;
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;

        // Record AFTER state: the placed block
        Block placed = event.getBlockPlaced();
        try {
            int blockId = SpigotConversionUtil
                    .fromBukkitBlockData(placed.getBlockData()).getGlobalId();
            recordBlockChangeFrame(session, placed.getX(), placed.getY(), placed.getZ(), blockId);
            LifeMod.getInstance().getDebugManager().log("replay", "Place AFTER at " + pos(placed)
                    + " type=" + placed.getType() + " id=" + blockId);
        } catch (Exception e) {
            LOGGER.warning("[ReplayBlockListener] Place AFTER failed: " + e.getMessage());
        }
    }

    // ─── MULTI-PLACE (door, bed…) ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMultiPlaceBefore(BlockMultiPlaceEvent event) {
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;
        for (BlockState state : event.getReplacedBlockStates()) {
            recordUndoFrameFromState(session, state);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMultiPlaceAfter(BlockMultiPlaceEvent event) {
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;
        for (BlockState state : event.getReplacedBlockStates()) {
            // Get current (placed) block at that position
            Block current = state.getWorld().getBlockAt(state.getX(), state.getY(), state.getZ());
            try {
                int blockId = SpigotConversionUtil
                        .fromBukkitBlockData(current.getBlockData()).getGlobalId();
                recordBlockChangeFrame(session, current.getX(), current.getY(), current.getZ(), blockId);
            } catch (Exception e) {
                LOGGER.warning("[ReplayBlockListener] MultiPlace AFTER failed: " + e.getMessage());
            }
        }
    }

    // ─── Frame writers ────────────────────────────────────────────────────────

    /**
     * Writes a 0xFA undo frame (state BEFORE the block changes).
     */
    private void recordUndoFrame(ReplaySession session, Block block) {
        try {
            int blockId = SpigotConversionUtil
                    .fromBukkitBlockData(block.getBlockData()).getGlobalId();
            writeRawFrame(session, buildUndoBytes(block.getX(), block.getY(), block.getZ(), blockId));
        } catch (Exception e) {
            LOGGER.warning("[ReplayBlockListener] recordUndoFrame failed: " + e.getMessage());
        }
    }

    private void recordUndoFrameFromState(ReplaySession session, BlockState state) {
        try {
            int blockId = SpigotConversionUtil
                    .fromBukkitBlockData(state.getBlockData()).getGlobalId();
            writeRawFrame(session, buildUndoBytes(state.getX(), state.getY(), state.getZ(), blockId));
        } catch (Exception e) {
            LOGGER.warning("[ReplayBlockListener] recordUndoFrameFromState failed: " + e.getMessage());
        }
    }

    /**
     * Writes a 0xFD block-change frame (state AFTER the block changes).
     * Format: [0xFD][0xFC_blockchange_packet_bytes]
     * We manually encode a BlockChange-like structure:
     *   [0xFD magic][int x][int y][int z][int globalBlockId]
     * PlaybackManager will decode this as a special 0xFD-block sub-type.
     *
     * Actually we use a simpler approach: store it as a raw custom packet
     * with magic 0xF9 so PlaybackManager can build a WrapperPlayServerBlockChange directly.
     */
    private void recordBlockChangeFrame(ReplaySession session, int x, int y, int z, int blockId) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(17);
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(ReplayCodec.BLOCK_CHANGE);  // magic: block change to replay
            dos.writeInt(x);
            dos.writeInt(y);
            dos.writeInt(z);
            dos.writeInt(blockId);
            writeRawFrame(session, baos.toByteArray());
        } catch (Exception e) {
            LOGGER.warning("[ReplayBlockListener] recordBlockChangeFrame failed: " + e.getMessage());
        }
    }

    private byte[] buildUndoBytes(int x, int y, int z, int blockId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(17);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(ReplayCodec.BLOCK_UNDO);
        dos.writeInt(x);
        dos.writeInt(y);
        dos.writeInt(z);
        dos.writeInt(blockId);
        return baos.toByteArray();
    }

    private void writeRawFrame(ReplaySession session, byte[] data) {
        session.queuePacket(data);
    }

    private String pos(Block b) {
        return b.getX() + "," + b.getY() + "," + b.getZ();
    }
}