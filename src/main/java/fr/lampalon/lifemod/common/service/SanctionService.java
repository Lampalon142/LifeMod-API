package fr.lampalon.lifemod.common.service;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.platform.bukkit.managers.database.DatabaseProvider;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SanctionService implements ISanctionService {

    private final DatabaseProvider db;
    private final IMessagingService messaging;

    public SanctionService(DatabaseProvider db) {
        this.db = db;
        this.messaging = ServiceRegistry.get(IMessagingService.class);
    }

    @Override
    public CompletableFuture<Sanction> applySanction(Sanction sanction) {
        return CompletableFuture.supplyAsync(() -> {
            db.saveSanction(sanction);
            
            // Notification via Redis pour synchronisation réseau
            if (messaging != null) {
                // Format: TYPE|PLAYER_UUID|SANCTION_UUID
                messaging.publish("lifemod:sanctions", "ADD|" + sanction.getType().name() + "|" + sanction.getPlayerUuid() + "|" + sanction.getUuid());
            }
            
            return sanction;
        });
    }

    @Override
    public CompletableFuture<Boolean> revokeSanction(UUID playerUuid, SanctionType type, UUID removedBy, String removedByName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            Sanction active = db.getActiveSanction(playerUuid, type);
            if (active == null) return false;

            active.revoke(removedBy, removedByName, reason);
            db.updateSanction(active);

            if (messaging != null) {
                messaging.publish("lifemod:sanctions", "REMOVE|" + type.name() + "|" + playerUuid);
            }

            return true;
        });
    }

    @Override
    public CompletableFuture<Sanction> getActiveSanction(UUID playerUuid, SanctionType type) {
        return CompletableFuture.supplyAsync(() -> db.getActiveSanction(playerUuid, type));
    }

    @Override
    public CompletableFuture<List<Sanction>> getHistory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> db.getSanctions(playerUuid));
    }

    @Override
    public void checkAutoPunish(UUID playerUuid, SanctionType type) {
        // TODO: Implémenter la logique Hybrid Auto-Punish définie dans le CDC
    }
}

