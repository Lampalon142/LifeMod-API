package fr.lampalon.lifemod.common.service;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.utils.NetworkUtil;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.common.database.DatabaseProvider;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SanctionService implements ISanctionService {

    private final DatabaseProvider db;
    private final IMessagingService messaging;
    private final ILifePlatform platform;
    private final ILangService lang;

    public SanctionService(DatabaseProvider db) {
        this.db = db;
        this.messaging = ServiceRegistry.get(IMessagingService.class);
        this.platform = ServiceRegistry.get(ILifePlatform.class);
        this.lang = ServiceRegistry.get(ILangService.class);
    }

    @Override
    public CompletableFuture<Sanction> applySanction(Sanction sanction) {
        return CompletableFuture.supplyAsync(() -> {
            db.saveSanction(sanction);
            
            // Mise à jour de la réputation IP si c'est un BAN
            if (sanction.getType() == SanctionType.BAN) {
                PlayerData data = db.getPlayerData(sanction.getPlayerUuid());
                if (data != null && data.getLastIp() != null) {
                    String ip = data.getLastIp();
                    DatabaseProvider.IPReputation rep = db.getIPReputation(ip);
                    int legits = db.getLegitimateAccountCount(ip);
                    int bans = (rep != null ? rep.bannedAccounts : 0) + 1;
                    db.updateIPReputation(ip, NetworkUtil.getSubnet(ip), legits, bans, System.currentTimeMillis(), false);
                }
            }

            // Broadcast local
            broadcastSanction(sanction);

            // Redis Sync
            if (messaging != null) {
                // ADD|TYPE|PLAYER_UUID|ISSUER_NAME|REASON|DURATION|SILENT|SERVER|CATEGORY
                String message = String.format("ADD|%s|%s|%s|%s|%d|%b|%s|%s",
                        sanction.getType().name(),
                        sanction.getPlayerUuid(),
                        sanction.getIssuerName(),
                        sanction.getReason(),
                        sanction.getDuration(),
                        sanction.isSilent(),
                        sanction.getServerName(),
                        sanction.getCategory()
                );
                messaging.publish("lifemod:sanctions", message);
            }

            // Auto-Punish check
            if (sanction.getType() == SanctionType.WARN || sanction.getType() == SanctionType.MUTE || sanction.getType() == SanctionType.BAN) {
                checkAutoPunish(sanction.getPlayerUuid(), sanction.getCategory());
            }
            
            return sanction;
        });
    }

    @Override
    public CompletableFuture<Boolean> revokeSanction(UUID playerUuid, SanctionType type, UUID removedBy, String removedByName, String reason, boolean silent) {
        return CompletableFuture.supplyAsync(() -> {
            Sanction active = db.getActiveSanction(playerUuid, platform.getPlayerName(playerUuid), type);
            if (active == null) return false;

            active.revoke(removedBy, removedByName, reason);
            db.updateSanction(active);

            // Redis Sync
            if (messaging != null) {
                // REMOVE|TYPE|PLAYER_UUID|REMOVED_BY_NAME|REASON|SILENT
                String message = String.format("REMOVE|%s|%s|%s|%s|%b",
                        type.name(),
                        playerUuid,
                        removedByName,
                        reason,
                        silent
                );
                messaging.publish("lifemod:sanctions", message);
            }

            broadcastRevoke(type, playerUuid, removedByName, reason, silent);

            return true;
        });
    }

    private void broadcastSanction(Sanction sanction) {
        platform.runTask(() -> {
            String targetName = platform.getPlayerName(sanction.getPlayerUuid());

            String path = "sanctions.broadcast." + sanction.getType().name().toLowerCase() + (sanction.isSilent() ? ".silent" : ".public");
            String message = lang.getMessage(path,
                    "%target%", targetName,
                    "%issuer%", sanction.getIssuerName(),
                    "%reason%", sanction.getReason(),
                    "%time%", TimeUtil.formatTime(sanction.getDuration()),
                    "%server%", sanction.getServerName());
            
            if (message.equals(path)) return;

            if (sanction.isSilent()) {
                platform.broadcast(message, "lifemod.sanctions.see-silent");
            } else {
                platform.broadcast(message, null);
            }
        });
    }

    private void broadcastRevoke(SanctionType type, UUID playerUuid, String removedByName, String reason, boolean silent) {
        platform.runTask(() -> {
            String targetName = platform.getPlayerName(playerUuid);

            String path = "sanctions.broadcast.un" + type.name().toLowerCase() + (silent ? ".silent" : ".public");
            String message = lang.getMessage(path,
                    "%target%", targetName,
                    "%issuer%", removedByName,
                    "%reason%", reason);
            
            if (message.equals(path)) {
                path = "sanctions.broadcast.un" + type.name().toLowerCase();
                message = lang.getMessage(path,
                        "%target%", targetName,
                        "%issuer%", removedByName,
                        "%reason%", reason);
            }
            
            if (message.equals(path)) return;

            if (silent) {
                platform.broadcast(message, "lifemod.sanctions.see-silent");
            } else {
                platform.broadcast(message, null);
            }
        });
    }

    @Override
    public CompletableFuture<Sanction> getActiveSanction(UUID playerUuid, String playerName, SanctionType type) {
        return CompletableFuture.supplyAsync(() -> db.getActiveSanction(playerUuid, playerName, type));
    }

    @Override
    public CompletableFuture<List<Sanction>> getHistory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> db.getSanctions(playerUuid));
    }

    @Override
    public CompletableFuture<List<Sanction>> getSanctionsIssuedBy(String issuerName, UUID issuerUuid) {
        return CompletableFuture.supplyAsync(() -> db.getSanctionsIssuedBy(issuerName, issuerUuid));
    }

    @Override
    public void checkAutoPunish(UUID playerUuid, String category) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        if (!config.getBoolean("modules.auto-punish.enabled", false)) return;

        getHistory(playerUuid).thenAccept(history -> {
            String mode = config.getString("modules.auto-punish.mode", "GLOBAL");
            long count;
            String command = null;

            if (mode.equalsIgnoreCase("CATEGORY") && category != null && !category.equals("Other") && !category.equals("None")) {
                count = history.stream()
                        .filter(s -> s.isActive() && !s.isExpired() && category.equalsIgnoreCase(s.getCategory()))
                        .count();
                command = config.getString("modules.auto-punish.categories." + category + "." + count, null);
            }

            // Fallback to GLOBAL if no category command found OR if mode is GLOBAL
            if (command == null) {
                // Pour le mode global, on compte uniquement les avertissements (WARNS) actifs
                count = history.stream()
                        .filter(s -> s.getType() == SanctionType.WARN && s.isActive() && !s.isExpired())
                        .count();
                command = config.getString("modules.auto-punish.global.thresholds." + count, null);
            }

            if (command != null) {
                String finalCommand = command.replace("%player%", platform.getPlayerName(playerUuid));
                platform.runTask(() -> {
                    platform.dispatchCommand(finalCommand);
                });
            }
        });
    }
}

