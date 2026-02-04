package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SanctionListener implements Listener {

    private final ISanctionService sanctionService;

    public SanctionListener() {
        this.sanctionService = ServiceRegistry.get(ISanctionService.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (sanctionService == null) return;

        // On bloque le thread de login pour vérifier la sanction (C'est un événement asynchrone, c'est fait pour)
        try {
            Sanction ban = sanctionService.getActiveSanction(event.getUniqueId(), SanctionType.BAN).get();
            
            if (ban != null) {
                if (ban.isExpired()) {
                    // La sanction est expirée, on pourrait la révoquer proprement ici ou attendre un cleanup
                    return;
                }

                String expiration = ban.isPermanent() ? "Permanent" : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(ban.getExpirationTime()));
                String message = MessageUtil.formatMessage(
                        "&c&lVous êtes banni de ce serveur ! " +
                        "&7Raison: &f" + ban.getReason() + " " +
                        "&7Expire le: &e" + expiration + " " +
                        "&7ID Sanction: &8#" + ban.getUuid().toString().substring(0, 8)
                );
                
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (sanctionService == null) return;

        try {
            Sanction mute = sanctionService.getActiveSanction(event.getPlayer().getUniqueId(), SanctionType.MUTE).get();
            if (mute != null && !mute.isExpired()) {
                event.setCancelled(true);
                String expiration = mute.isPermanent() ? "Jamais" : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(mute.getExpirationTime()));
                event.getPlayer().sendMessage(MessageUtil.formatMessage(
                        "&cVous êtes réduit au silence !\n" +
                        "&7Raison: &f" + mute.getReason() + "\n" +
                        "&7Expire le: &e" + expiration
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

