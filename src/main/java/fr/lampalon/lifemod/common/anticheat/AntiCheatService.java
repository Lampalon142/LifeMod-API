package fr.lampalon.lifemod.common.anticheat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import fr.lampalon.lifemod.common.anticheat.check.Check;
import fr.lampalon.lifemod.common.anticheat.check.combat.AutoClicker;
import fr.lampalon.lifemod.common.anticheat.check.combat.Hitbox;
import fr.lampalon.lifemod.common.anticheat.check.combat.Reach;
import fr.lampalon.lifemod.common.anticheat.check.movement.Fly;
import fr.lampalon.lifemod.common.anticheat.check.movement.NoFall;
import fr.lampalon.lifemod.common.anticheat.check.movement.Timer;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service managing the lifecycle of the AntiCheat module.
 *
 * Packet wrapping contract:
 *   - INTERACT_ENTITY                          → WrapperPlayClientInteractEntity
 *   - PLAYER_FLYING / POSITION /
 *     POSITION_AND_ROTATION / ROTATION         → WrapperPlayClientPlayerFlying
 *   - Everything else                          → raw PacketReceiveEvent (fallback)
 *
 * Every Check must cast its context according to the packets it declared
 * in getSupportedPackets(). As long as that contract is respected, all
 * checks (combat + movement) work in harmony without interfering with
 * each other.
 */
public final class AntiCheatService implements PacketListener {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final ILifePlatform platform;

    /** Per-player runtime data, thread-safe. */
    private final Map<UUID, ACPlayerData> playerDataMap = new ConcurrentHashMap<>();

    /** Flat list of all registered checks (kept for easy iteration / debug). */
    private final List<Check> checks = new CopyOnWriteArrayList<>();

    /**
     * Fast-lookup map: packet type → list of checks interested in that packet.
     * Built once at init time; reads are concurrent-safe.
     */
    private final Map<PacketTypeCommon, List<Check>> checkMap = new ConcurrentHashMap<>();

    /** Thread pool used for async checks (e.g. Reach, Hitbox). */
    private final ExecutorService asyncProcessor;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AntiCheatService(ILifePlatform platform) {
        this.platform = platform;
        this.asyncProcessor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "LifeMod-AC-Processor");
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Called once when the module is enabled. */
    public void init() {
        registerChecks();
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.LOW);
        platform.logInfo("[AC] AntiCheat initialized with " + checks.size() + " check(s).");
    }

    /** Called once when the module is disabled / server shuts down. */
    public void terminate() {
        asyncProcessor.shutdown();
        playerDataMap.clear();
        checks.clear();
        checkMap.clear();
        platform.logInfo("[AC] AntiCheat terminated.");
    }

    // -------------------------------------------------------------------------
    // Check registration
    // -------------------------------------------------------------------------

    /**
     * Registers all built-in checks.
     * To add a new check, simply call registerCheck(new MyCheck(config)) here.
     */
    private void registerChecks() {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);

        // --- Combat ---
        registerCheck(new AutoClicker(config));
        registerCheck(new Reach(config));
        registerCheck(new Hitbox(config));

        // --- Movement ---
        registerCheck(new Timer(config));
        registerCheck(new Fly(config));
        registerCheck(new NoFall(config));
    }

    /**
     * Registers a single check and maps it to every packet type it declared.
     *
     * @param check the check to register
     */
    public void registerCheck(Check check) {
        checks.add(check);
        for (PacketTypeCommon type : check.getSupportedPackets()) {
            checkMap.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(check);
        }
    }

    // -------------------------------------------------------------------------
    // Packet handling
    // -------------------------------------------------------------------------

    /**
     * Entry point for every incoming client packet.
     *
     * <p>Wrapping strategy (see class-level javadoc):
     * <ul>
     *   <li>INTERACT_ENTITY → {@link WrapperPlayClientInteractEntity}</li>
     *   <li>Movement packets  → {@link WrapperPlayClientPlayerFlying}</li>
     *   <li>Anything else     → raw {@link PacketReceiveEvent}</li>
     * </ul>
     *
     * The wrapped object is created ONCE per packet and shared across every
     * interested check to avoid redundant allocations.
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Guard: ignore packets from anonymous / unauthenticated users
        if (event.getUser() == null || event.getUser().getUUID() == null) return;

        UUID uuid = event.getUser().getUUID();
        ACPlayerData data = getPlayerData(uuid);
        if (data == null) return; // player not tracked (e.g. not yet joined)

        PacketTypeCommon type = event.getPacketType();
        List<Check> interestedChecks = checkMap.get(type);
        if (interestedChecks == null || interestedChecks.isEmpty()) return;

        // --- Wrap the packet once, reuse across all checks ---
        final Object context = wrapPacket(type, event);

        // --- Dispatch to each interested check ---
        for (Check check : interestedChecks) {
            if (!check.isEnabled()) continue;

            if (check.isAsync()) {
                asyncProcessor.execute(() -> handleCheck(check, uuid, data, context));
            } else {
                handleCheck(check, uuid, data, context);
            }
        }
    }

    /**
     * Wraps a raw {@link PacketReceiveEvent} into the appropriate typed wrapper
     * so that checks receive a convenient, already-parsed object.
     *
     * <p>Contract respected here must be mirrored in each check's cast:
     * <pre>
     *   INTERACT_ENTITY                        → WrapperPlayClientInteractEntity
     *   PLAYER_FLYING / POSITION /
     *     POSITION_AND_ROTATION / ROTATION     → WrapperPlayClientPlayerFlying
     *   other                                  → PacketReceiveEvent (raw)
     * </pre>
     */
    private Object wrapPacket(PacketTypeCommon type, PacketReceiveEvent event) {
        // --- Combat packets ---
        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            return new WrapperPlayClientInteractEntity(event);
        }

        // --- Movement packets ---
        // WrapperPlayClientPlayerFlying covers PLAYER_FLYING, PLAYER_POSITION,
        // PLAYER_POSITION_AND_ROTATION, and PLAYER_ROTATION because PacketEvents
        // maps them all to the same wrapper (they share the same fields).
        if (type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION) {
            return new WrapperPlayClientPlayerFlying(event);
        }

        // --- Fallback: pass raw event (check must handle it itself) ---
        return event;
    }

    /**
     * Delegates packet processing to a single check, swallowing exceptions so
     * that a buggy check can never break the entire pipeline.
     */
    private void handleCheck(Check check, UUID uuid, ACPlayerData data, Object context) {
        try {
            check.onHandle(uuid, data, context);
        } catch (Exception e) {
            platform.logInfo("§c[AC] Error in check '" + check.getName() + "': " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Player data management
    // -------------------------------------------------------------------------

    /** Returns the {@link ACPlayerData} for the given UUID, or {@code null} if not tracked. */
    public ACPlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    /** Creates and stores a fresh {@link ACPlayerData} entry for a joining player. */
    public void addPlayerData(UUID uuid) {
        playerDataMap.put(uuid, new ACPlayerData(uuid));
    }

    /** Removes the {@link ACPlayerData} entry for a leaving player. */
    public void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }
}