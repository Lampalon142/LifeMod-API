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
        PlayerData data = new PlayerData(uuid, name, ip, System.currentTimeMillis());
        LifeMod.getInstance().getDatabaseManager().getDatabaseProvider().savePlayerData(data);

        // Vérification des bans
        ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
        Sanction activeBan = sanctionService.getActiveSanction(uuid, name, SanctionType.BAN).join();

        if (activeBan != null) {
            String message = LifeMod.getInstance().getLangConfig().getString("sanctions.ban.login", "&cYou have been banned!\n\nReason: &f%reason%");
            message = message
                    .replace("%reason%", activeBan.getReason())
                    .replace("%issuer%", activeBan.getIssuerName())
                    .replace("%time%", TimeUtil.formatTime(activeBan.getExpirationTime() - System.currentTimeMillis()))
                    .replace("%server%", activeBan.getServerName());
            
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MessageUtil.formatMessage(message));
        }
    }
}

