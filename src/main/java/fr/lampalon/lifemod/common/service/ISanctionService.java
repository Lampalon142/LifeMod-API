package fr.lampalon.lifemod.common.service;

import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ISanctionService {
    
    /**
     * Applique une nouvelle sanction à un joueur
     */
    CompletableFuture<Sanction> applySanction(Sanction sanction);

    /**
     * Révoque une sanction active
     */
    CompletableFuture<Boolean> revokeSanction(UUID playerUuid, SanctionType type, UUID removedBy, String removedByName, String reason);

    /**
     * Vérifie si une sanction de ce type est active pour le joueur
     */
    CompletableFuture<Sanction> getActiveSanction(UUID playerUuid, SanctionType type);

    /**
     * Récupère l'historique complet d'un joueur
     */
    CompletableFuture<List<Sanction>> getHistory(UUID playerUuid);

    /**
     * Vérifie et applique l'Auto-Punish si nécessaire
     */
    void checkAutoPunish(UUID playerUuid, SanctionType type);
}

