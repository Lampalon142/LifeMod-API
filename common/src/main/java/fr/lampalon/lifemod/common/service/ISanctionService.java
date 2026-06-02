package fr.lampalon.lifemod.common.service;

import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ISanctionService {
    
    /**
     * Applies a new sanction to a player
     */
    CompletableFuture<Sanction> applySanction(Sanction sanction);

    /**
     * Revokes an active sanction
     */
    CompletableFuture<Boolean> revokeSanction(UUID playerUuid, SanctionType type, UUID removedBy, String removedByName, String reason, boolean silent);

    /**
     * Checks if a sanction of this type is active for the player
     */
    CompletableFuture<Sanction> getActiveSanction(UUID playerUuid, String playerName, SanctionType type);

    /**
     * Retrieves the complete history of a player
     */
    CompletableFuture<List<Sanction>> getHistory(UUID playerUuid);

    /**
     * Retrieves sanctions issued by a moderator
     */
    CompletableFuture<List<Sanction>> getSanctionsIssuedBy(String issuerName, UUID issuerUuid);

    /**
     * Retrieves active sanctions for a batch of players
     */
    CompletableFuture<Map<UUID, Sanction>> getActiveSanctions(Collection<UUID> playerUuids, String playerName, SanctionType type);

    /**
     * Checks and applies Auto-Punish if necessary
     */
    void checkAutoPunish(UUID playerUuid, String category);
}

