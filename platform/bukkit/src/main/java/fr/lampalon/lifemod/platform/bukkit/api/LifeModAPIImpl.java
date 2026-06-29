package fr.lampalon.lifemod.platform.bukkit.api;

import fr.lampalon.lifemod.api.LifeModAPI;
import fr.lampalon.lifemod.api.sanction.ISanctionService;
import fr.lampalon.lifemod.api.sanction.Sanction;
import fr.lampalon.lifemod.api.sanction.SanctionType;
import fr.lampalon.lifemod.api.staff.IVanishService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LifeModAPIImpl implements LifeModAPI {

    private final ISanctionService sanctionService;
    private final IVanishService vanishService;

    public LifeModAPIImpl(LifeMod plugin) {
        this.sanctionService = new ApiSanctionService();
        this.vanishService = new ApiVanishService(plugin);
    }

    @Override
    public ISanctionService getSanctionService() {
        return sanctionService;
    }

    @Override
    public IVanishService getVanishService() {
        return vanishService;
    }

    // ─── Sanction adapter ─────────────────────────────────────────────────────

    private static class ApiSanctionService implements ISanctionService {
        private final fr.lampalon.lifemod.common.service.ISanctionService delegate =
                ServiceRegistry.get(fr.lampalon.lifemod.common.service.ISanctionService.class);

        @Override
        public CompletableFuture<Sanction> applySanction(Sanction sanction) {
            return delegate.applySanction(toInternal(sanction))
                    .thenApply(LifeModAPIImpl::toApi);
        }

        @Override
        public CompletableFuture<Boolean> revokeSanction(UUID playerUuid, SanctionType type,
                                                         UUID removedBy, String removedByName,
                                                         String reason, boolean silent) {
            return delegate.revokeSanction(playerUuid, toInternal(type),
                    removedBy, removedByName, reason, silent);
        }

        @Override
        public CompletableFuture<Sanction> getActiveSanction(UUID playerUuid, String playerName, SanctionType type) {
            return delegate.getActiveSanction(playerUuid, playerName, toInternal(type))
                    .thenApply(s -> s != null ? toApi(s) : null);
        }

        @Override
        public CompletableFuture<List<Sanction>> getHistory(UUID playerUuid) {
            return delegate.getHistory(playerUuid)
                    .thenApply(list -> list.stream().map(LifeModAPIImpl::toApi).collect(Collectors.toList()));
        }

        @Override
        public CompletableFuture<List<Sanction>> getSanctionsIssuedBy(String issuerName, UUID issuerUuid) {
            return delegate.getSanctionsIssuedBy(issuerName, issuerUuid)
                    .thenApply(list -> list.stream().map(LifeModAPIImpl::toApi).collect(Collectors.toList()));
        }

        @Override
        public CompletableFuture<Map<UUID, Sanction>> getActiveSanctions(Collection<UUID> playerUuids,
                                                                         String playerName, SanctionType type) {
            return delegate.getActiveSanctions(playerUuids, playerName, toInternal(type))
                    .thenApply(map -> map.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> toApi(e.getValue()))));
        }
    }

    // ─── Vanish adapter ───────────────────────────────────────────────────────

    private static class ApiVanishService implements IVanishService {
        private final LifeMod plugin;

        ApiVanishService(LifeMod plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean isVanished(UUID uuid) {
            return plugin.getVanishService().isVanished(uuid);
        }

        @Override
        public void setVanished(UUID uuid, boolean vanished, boolean silent) {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getVanishService().setVanished(player, vanished, silent);
            }
        }

        @Override
        public Set<UUID> getVanishedPlayers() {
            return plugin.getVanishService().getVanishedPlayers();
        }
    }

    // ─── Model converters ─────────────────────────────────────────────────────

    private static fr.lampalon.lifemod.common.model.SanctionType toInternal(SanctionType type) {
        return fr.lampalon.lifemod.common.model.SanctionType.valueOf(type.name());
    }

    private static SanctionType toApi(fr.lampalon.lifemod.common.model.SanctionType type) {
        return SanctionType.valueOf(type.name());
    }

    private static fr.lampalon.lifemod.common.model.Sanction toInternal(Sanction s) {
        return new fr.lampalon.lifemod.common.model.Sanction(
                s.getUuid(), s.getPlayerUuid(), s.getPlayerName(),
                s.getIssuerUuid(), s.getIssuerName(),
                s.getServerName(), s.getCategory(),
                toInternal(s.getType()), s.getReason(),
                s.getCreatedAt(), s.getDuration(), s.isSilent(), true
        );
    }

    private static Sanction toApi(fr.lampalon.lifemod.common.model.Sanction s) {
        return new Sanction(
                s.getUuid(), s.getPlayerUuid(), s.getPlayerName(),
                s.getIssuerUuid(), s.getIssuerName(),
                s.getServerName(), s.getCategory(),
                toApi(s.getType()), s.getReason(),
                s.getCreatedAt(), s.getDuration(), s.isSilent(), s.isActive(),
                s.getEvidence(), s.getRemovedByUuid(), s.getRemovedByName(),
                s.getRemoveReason(), s.getRemovedAt()
        );
    }
}
