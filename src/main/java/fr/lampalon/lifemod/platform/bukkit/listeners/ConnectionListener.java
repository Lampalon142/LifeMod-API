package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.common.database.DatabaseProvider;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class ConnectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ip = event.getAddress().getHostAddress();

        // Enregistrement async des données du joueur
        DatabaseProvider db = LifeMod.getInstance().getDatabaseManager().getDatabaseProvider();
        PlayerData existing = db.getPlayerData(uuid);
        
        long firstSeen = existing != null ? existing.getFirstSeen() : System.currentTimeMillis();
        int sessionCount = existing != null ? existing.getSessionCount() + 1 : 1;
        boolean inStaffMode = existing != null && existing.isInStaffMode();

        PlayerData data = new PlayerData(uuid, name, ip, System.currentTimeMillis(), firstSeen, sessionCount, inStaffMode);
        db.savePlayerData(data);

        // Mise à jour de la réputation IP (comptes légitimes)
        if (sessionCount == 6) { // On vient de passer le seuil des 5 sessions
            String subnet = getSubnet(ip);
            DatabaseProvider.IPReputation rep = db.getIPReputation(ip);
            int legits = db.getLegitimateAccountCount(ip);
            int bans = rep != null ? rep.bannedAccounts : 0;
            db.updateIPReputation(ip, subnet, legits, bans, System.currentTimeMillis(), false);
        }

        // Vérification des bans
        ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
        Sanction activeBan = sanctionService.getActiveSanction(uuid, name, SanctionType.BAN).join();

        if (activeBan != null) {
            String message = LifeMod.getInstance().getLangConfig().getString("sanctions.ban.login");
            message = message
                    .replace("%reason%", activeBan.getReason())
                    .replace("%issuer%", activeBan.getIssuerName())
                    .replace("%time%", TimeUtil.formatTime(activeBan.getExpirationTime() - System.currentTimeMillis()))
                    .replace("%server%", activeBan.getServerName());
            
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MessageUtil.formatMessage(message));
        }
    }

    private String getSubnet(String ip) {
        int lastDot = ip.lastIndexOf('.');
        if (lastDot == -1) return ip;
        return ip.substring(0, lastDot);
    }
}

