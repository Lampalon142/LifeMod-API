package fr.lampalon.lifemod.platform.bukkit

import fr.lampalon.lifemod.platform.bukkit.managers.database.DatabaseProvider;.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class ConnectionListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ip = event.getAddress().getHostAddress();

        // Enregistrement async des données du joueur
        PlayerData data = new PlayerData(uuid, name, ip, System.currentTimeMillis());
        LifeMod.getInstance().getDatabaseManager().getDatabaseProvider().savePlayerData(data);
    }
}

