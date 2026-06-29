package fr.lampalon.lifemod.api.sanction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ISanctionService {

    CompletableFuture<Sanction> applySanction(Sanction sanction);

    CompletableFuture<Boolean> revokeSanction(UUID playerUuid, SanctionType type,
                                              UUID removedBy, String removedByName,
                                              String reason, boolean silent);

    CompletableFuture<Sanction> getActiveSanction(UUID playerUuid, String playerName, SanctionType type);

    CompletableFuture<List<Sanction>> getHistory(UUID playerUuid);

    CompletableFuture<List<Sanction>> getSanctionsIssuedBy(String issuerName, UUID issuerUuid);

    CompletableFuture<Map<UUID, Sanction>> getActiveSanctions(Collection<UUID> playerUuids,
                                                              String playerName, SanctionType type);
}
