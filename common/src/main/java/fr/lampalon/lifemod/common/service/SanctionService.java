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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SanctionService implements ISanctionService {

    private static final ExecutorService LIFEMOD_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "LifeMod-Async");
        t.setDaemon(true);
        return t;
    });

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

            if (messaging != null) {
                String message = String.format("ADD|%s|%s|%s|%s|%s|%d|%b|%s|%s",
                        sanction.getType().name(),
                        sanction.getPlayerUuid(),
                        sanction.getPlayerName(),
                        sanction.getIssuerName(),
                        sanction.getReason(),
                        sanction.getDuration(),
                        sanction.isSilent(),
                        sanction.getServerName(),
                        sanction.getCategory()
                );
                messaging.publish("lifemod:sanctions", message);
            } else {
                broadcastSanction(sanction);
            }

            if (sanction.getType() == SanctionType.WARN || sanction.getType() == SanctionType.MUTE || sanction.getType() == SanctionType.BAN) {
                checkAutoPunish(sanction.getPlayerUuid(), sanction.getCategory());
            }
            
            return sanction;
        }, LIFEMOD_EXECUTOR);
    }

    @Override
    public CompletableFuture<Boolean> revokeSanction(UUID playerUuid, SanctionType type, UUID removedBy, String removedByName, String reason, boolean silent) {
        return CompletableFuture.supplyAsync(() -> {
            List<Sanction> allActive = db.getActiveSanctions(playerUuid);
            List<Sanction> toRevoke = allActive.stream()
                    .filter(s -> s.getType() == type)
                    .collect(Collectors.toList());
            if (toRevoke.isEmpty()) return false;

            for (Sanction active : toRevoke) {
                active.revoke(removedBy, removedByName, reason);
                db.updateSanction(active);
            }

            if (messaging != null) {
                String message = String.format("REMOVE|%s|%s|%s|%s|%s|%b",
                        type.name(),
                        playerUuid,
                        toRevoke.get(0).getPlayerName(),
                        removedByName,
                        reason,
                        silent
                );
                messaging.publish("lifemod:sanctions", message);
            } else {
                broadcastRevoke(type, playerUuid, removedByName, reason, silent);
            }

            return true;
        }, LIFEMOD_EXECUTOR);
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
        return CompletableFuture.supplyAsync(() -> db.getActiveSanction(playerUuid, playerName, type), LIFEMOD_EXECUTOR);
    }

    @Override
    public CompletableFuture<Map<UUID, Sanction>> getActiveSanctions(Collection<UUID> playerUuids, String playerName, SanctionType type) {
        return CompletableFuture.supplyAsync(() -> {
            List<Sanction> sanctions = db.getActiveSanctions(playerUuids, type);
            return sanctions.stream().collect(Collectors.toMap(Sanction::getPlayerUuid, s -> s));
        }, LIFEMOD_EXECUTOR);
    }

    @Override
    public CompletableFuture<List<Sanction>> getHistory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> db.getSanctions(playerUuid), LIFEMOD_EXECUTOR);
    }

    @Override
    public CompletableFuture<List<Sanction>> getSanctionsIssuedBy(String issuerName, UUID issuerUuid) {
        return CompletableFuture.supplyAsync(() -> db.getSanctionsIssuedBy(issuerName, issuerUuid), LIFEMOD_EXECUTOR);
    }

    @Override
    public void checkAutoPunish(UUID playerUuid, String category) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        if (!config.getBoolean("modules.auto-punish.enabled", false)) return;

        getHistory(playerUuid).thenAccept(history -> {
            String mode = config.getString("modules.auto-punish.mode", "GLOBAL");
            long count = 0;
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

            if (command != null && !history.isEmpty()) {
                String playerName = history.get(0).getPlayerName();
                String lowerCmd = command.toLowerCase();
                boolean isBan = lowerCmd.startsWith("ban");
                boolean isMute = lowerCmd.startsWith("mute");
                if (isBan || isMute) {
                    Sanction active = db.getActiveSanction(playerUuid, playerName, isBan ? SanctionType.BAN : SanctionType.MUTE);
                    if (active != null) return;
                }
                String finalCommand = command.replace("%player%", playerName);
                platform.runTask(() -> {
                    platform.dispatchCommand(finalCommand);
                });
            }
        }).exceptionally(ex -> { ex.printStackTrace(); return null; });
    }

    private SanctionType parseTypeFromCommand(String command) {
        if (command == null || command.isEmpty()) return SanctionType.WARN;
        String firstWord = command.split(" ")[0].toLowerCase();
        switch (firstWord) {
            case "ban": return SanctionType.BAN;
            case "kick": return SanctionType.KICK;
            case "mute": return SanctionType.MUTE;
            default: return SanctionType.WARN;
        }
    }

    private long parseDurationFromCommand(String command) {
        if (command == null || command.isEmpty()) return 0;
        String[] parts = command.split(" ");
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.equals("%player%")) continue;
            Matcher m = Pattern.compile("(\\d+)([smhd])").matcher(p);
            if (m.matches()) {
                long amount = Long.parseLong(m.group(1));
                switch (m.group(2)) {
                    case "s": return amount * 1000;
                    case "m": return amount * 60000;
                    case "h": return amount * 3600000;
                    case "d": return amount * 86400000;
                }
            }
            if (p.equalsIgnoreCase("permanent")) return 0;
        }
        return 0;
    }
}

