package fr.lampalon.lifemod.common.anticheat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import fr.lampalon.lifemod.common.anticheat.check.Check;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Service managing the lifecycle of the AntiCheat module.
 * Responsibilities: PlayerData management, Packet interception, Check registration.
 */
public final class AntiCheatService implements PacketListener {

    private final ILifePlatform platform;
    private final Map<UUID, ACPlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final List<Check> checks = new ArrayList<>();
    private final Map<PacketTypeCommon, List<Check>> checkMap = new ConcurrentHashMap<>();
    private final ExecutorService asyncProcessor;

    public AntiCheatService(ILifePlatform platform) {
        this.platform = platform;
        this.asyncProcessor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> {
            Thread t = new Thread(r, "LifeMod-AC-Processor");
            t.setDaemon(true);
            return t;
        });
    }

    public void init() {
        registerChecks();
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.LOW);
    }

    private void registerChecks() {
        fr.lampalon.lifemod.common.service.IConfigurationService config = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.IConfigurationService.class);
        registerCheck(new fr.lampalon.lifemod.common.anticheat.check.combat.AutoClicker(config));
        registerCheck(new fr.lampalon.lifemod.common.anticheat.check.combat.Reach(config));
        registerCheck(new fr.lampalon.lifemod.common.anticheat.check.combat.Hitbox(config));
        registerCheck(new fr.lampalon.lifemod.common.anticheat.check.combat.Aimbot(config));
        registerCheck(new fr.lampalon.lifemod.common.anticheat.check.movement.Timer(config));
    }

    public void terminate() {
        com.github.retrooper.packetevents.event.PacketListener listener = this;
        PacketEvents.getAPI().getEventManager().unregisterListener((PacketListenerCommon) listener);
        asyncProcessor.shutdown();
        playerDataMap.clear();
        checks.clear();
        checkMap.clear();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getUser() == null || event.getUser().getUUID() == null) return;
        UUID uuid = event.getUser().getUUID();
        ACPlayerData data = getPlayerData(uuid);
        if (data == null) return;

        PacketTypeCommon type = event.getPacketType();
        List<Check> interestedChecks = checkMap.get(type);
        if (interestedChecks == null) return;

        for (Check check : interestedChecks) {
            if (!check.isEnabled()) continue;

            if (check.isAsync()) {
                asyncProcessor.execute(() -> handleCheck(check, uuid, data, event));
            } else {
                handleCheck(check, uuid, data, event);
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // Required by interface
    }

    private void handleCheck(Check check, UUID uuid, ACPlayerData data, PacketReceiveEvent event) {
        try {
            check.onHandle(uuid, data, event);
        } catch (Exception e) {
            platform.logInfo("§c[Feat-AC] Error while handling " + check.getName() + ": " + e.getMessage());
        }
    }

    public ACPlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void addPlayerData(UUID uuid) {
        playerDataMap.put(uuid, new ACPlayerData(uuid));
    }

    public void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public void registerCheck(Check check) {
        checks.add(check);
        for (PacketTypeCommon type : check.getSupportedPackets()) {
            checkMap.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(check);
        }
    }
}
