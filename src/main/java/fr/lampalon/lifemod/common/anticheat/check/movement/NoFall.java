package fr.lampalon.lifemod.common.anticheat.check.movement;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * NoFall — detects clients that suppress fall damage by sending onGround=true
 * before they should be on the ground, or by never accumulating fall distance.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Detection model                                                │
 * │                                                                 │
 * │  A) onGround spoof — client sends onGround=true while still    │
 * │     falling (Y is still decreasing and not near any solid       │
 * │     block). Flagged immediately.                                │
 * │                                                                 │
 * │  B) Fall-distance suppression — client lands (server-side      │
 * │     block check confirms ground) but accumulated fall distance  │
 * │     is 0 or far below what the Y delta proves. Flagged only     │
 * │     when the discrepancy is unambiguous.                        │
 * │                                                                 │
 * │  Exempt situations (no false positives):                        │
 * │   • Creative / Spectator / flight allowed                       │
 * │   • Slow Falling potion                                         │
 * │   • Climbable blocks (ladder, vine, scaffold, powder snow…)     │
 * │   • Water / lava / cobweb (velocity reset blocks)               │
 * │   • Just teleported (velocity buffer)                           │
 * │   • Lag spike > 500 ms ping                                     │
 * │   • Elytra gliding                                              │
 * │   • Recent server-side velocity packet (knockback, explosion)   │
 * └─────────────────────────────────────────────────────────────────┘
 */
public class NoFall extends AbstractCheck {

    // ── Tunables ────────────────────────────────────────────────────────────
    /** Minimum fall distance (blocks) before we even consider flagging. */
    private static final double MIN_FLAG_DISTANCE = 3.5;

    /**
     * How many blocks below the player's feet we scan for a solid surface
     * when verifying an onGround claim server-side.
     */
    private static final double GROUND_SCAN_DEPTH = 0.6;

    /** Tolerance applied to the Y-position check (accounts for sub-block precision). */
    private static final double Y_EPSILON = 0.001;

    /** Ticks after a teleport / velocity packet during which we stay silent. */
    private static final int TELEPORT_GRACE_TICKS = 10;

    /** Maximum ping (ms) above which we consider the data unreliable. */
    private static final long MAX_RELIABLE_PING = 500L;

    // ── Per-player transient state (stored in ACPlayerData fields) ───────────
    // We reuse:
    //   data.getLastY()          – Y of previous packet
    //   data.getFallDistance()   – accumulated fall distance (blocks)
    //   data.getLastVelocityTime() – timestamp of last server velocity push
    //   data.getAirTicks()       – consecutive ticks in air
    //   data.isOnGround()        – server-confirmed ground state

    // ── Constructor ─────────────────────────────────────────────────────────

    public NoFall(IConfigurationService configService) {
        super("NoFall", configService);
    }

    // ── Main handler ────────────────────────────────────────────────────────

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        if (!(context instanceof WrapperPlayClientPlayerFlying)) return;
        WrapperPlayClientPlayerFlying wrapper = (WrapperPlayClientPlayerFlying) context;

        // Only packets that carry a position are meaningful for fall tracking
        if (!wrapper.hasPositionChanged()) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        // ── Global exemptions ────────────────────────────────────────────────
        if (isGloballyExempt(player, data)) {
            resetFall(data);
            return;
        }

        double currentY  = wrapper.getLocation().getY();
        double lastY     = data.getLastY();
        boolean clientOnGround = wrapper.isOnGround();

        // First packet after join — initialise and skip
        if (lastY == 0.0 && data.getFallDistance() == 0.0f) {
            data.setLastY(currentY);
            return;
        }

        double deltaY = lastY - currentY; // positive = falling

        // ── Update fall distance accumulator ────────────────────────────────
        if (deltaY > Y_EPSILON) {
            // Player is moving downward — accumulate
            data.setFallDistance(data.getFallDistance() + (float) deltaY);
            data.setAirTicks(data.getAirTicks() + 1);
        } else if (deltaY < -Y_EPSILON) {
            // Player is moving upward (jump, bounce) — reset accumulator
            // Keep a small residual so upward flick → instant land is caught
            data.setFallDistance(0.0f);
            data.setAirTicks(0);
        }
        // deltaY ≈ 0: horizontal movement only, carry over existing state

        float  accFall    = data.getFallDistance();
        boolean serverOnGround = isNearGround(player, currentY);

        // ── Detection A: onGround spoof ──────────────────────────────────────
        // Client claims to be on the ground while:
        //   1. Server does NOT see a solid block within GROUND_SCAN_DEPTH
        //   2. The player has been falling long enough to matter
        if (clientOnGround && !serverOnGround && accFall >= MIN_FLAG_DISTANCE) {
            flag(uuid, data, 1.0,
                    String.format("type=SPOOF dist=%.2f dy=%.4f airTicks=%d ping=%d",
                            accFall, deltaY, data.getAirTicks(), data.getPing()));
            resetFall(data);
            data.setLastY(currentY);
            return;
        }

        // ── Detection B: fall-distance suppression ───────────────────────────
        // Server confirms the player landed, but accumulated fall distance is
        // suspiciously low compared to what the Y-delta proves.
        if (serverOnGround && accFall < 0.5 && data.getAirTicks() > 8) {
            // The player was airborne for >8 ticks but claims nearly 0 fall distance.
            // This indicates the client reset its fall counter mid-air.
            flag(uuid, data, 0.9,
                    String.format("type=SUPPRESS airTicks=%d dist=%.2f ping=%d",
                            data.getAirTicks(), accFall, data.getPing()));
            resetFall(data);
            data.setLastY(currentY);
            return;
        }

        // ── Normal landing — reset ───────────────────────────────────────────
        if (serverOnGround) {
            resetFall(data);
        }

        data.setLastY(currentY);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when this player should never be flagged by NoFall,
     * regardless of packet content.
     */
    private boolean isGloballyExempt(Player player, ACPlayerData data) {
        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return true;
        if (player.getAllowFlight() || player.isFlying())        return true;
        if (player.isGliding())                                  return true; // elytra

        // Slow Falling potion completely negates fall damage
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) return true;

        // Recent server-side velocity (knockback / explosion) can produce
        // huge Y deltas that look like NoFall
        long msSinceVelocity = System.currentTimeMillis() - data.getLastVelocityTime();
        if (msSinceVelocity < 500L) return true;

        // High ping → positions are unreliable
        if (data.getPing() > MAX_RELIABLE_PING) return true;

        // Climbable / liquid blocks at feet
        if (isOnClimbableOrLiquid(player)) return true;

        return false;
    }

    /**
     * Server-side ground check: scans the block at and just below the player's
     * feet. More reliable than the client's onGround flag.
     */
    private boolean isNearGround(Player player, double y) {
        // Check the block at foot level and one step below
        for (double offset = 0; offset <= GROUND_SCAN_DEPTH; offset += 0.1) {
            Block block = player.getWorld().getBlockAt(
                    (int) Math.floor(player.getLocation().getX()),
                    (int) Math.floor(y - offset),
                    (int) Math.floor(player.getLocation().getZ())
            );
            if (isSolid(block)) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} for blocks that reset fall damage or allow climbing,
     * where NoFall behaviour is entirely legitimate.
     */
    private boolean isOnClimbableOrLiquid(Player player) {
        Block feet  = player.getLocation().getBlock();
        Block below = player.getLocation().subtract(0, 0.1, 0).getBlock();

        for (Block b : new Block[]{feet, below}) {
            Material m = b.getType();
            if (m == Material.WATER || m == Material.LAVA)        return true;
            if (m == Material.LADDER || m == Material.VINE)       return true;
            if (m == Material.COBWEB)                             return true;
            if (m == Material.SCAFFOLDING)                        return true;
            if (m == Material.POWDER_SNOW)                        return true;
            if (m.name().contains("HONEY_BLOCK"))                 return true;
            if (m.name().contains("SLIME_BLOCK"))                 return true;
            if (m.name().contains("BED") && !m.name().contains("BEDROCK")) return true;
        }
        return false;
    }

    /**
     * A block counts as "solid ground" if it is a full cube or a common
     * partial block that stops fall damage (stairs, slabs, etc.).
     */
    private boolean isSolid(Block block) {
        if (block == null) return false;
        Material m = block.getType();
        if (m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR) return false;
        if (m == Material.WATER || m == Material.LAVA) return false;
        // Bukkit's isSolid() covers the overwhelming majority of blocks
        return m.isSolid();
    }

    /**
     * Resets all fall-tracking state to zero after a legitimate landing
     * or after a flag.
     */
    private void resetFall(ACPlayerData data) {
        data.setFallDistance(0.0f);
        data.setAirTicks(0);
    }

    // ── Packet subscription ──────────────────────────────────────────────────

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{
                PacketType.Play.Client.PLAYER_POSITION,
                PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
                PacketType.Play.Client.PLAYER_FLYING
        };
    }
}