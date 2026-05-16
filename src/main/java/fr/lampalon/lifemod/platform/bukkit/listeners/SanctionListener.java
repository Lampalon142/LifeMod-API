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
            Sanction ban = sanctionService.getActiveSanction(event.getUniqueId(), event.getName(), SanctionType.BAN).get();
            
            if (ban != null) {
                if (ban.isExpired()) {
                    // La sanction est expirée, on pourrait la révoquer proprement ici ou attendre un cleanup
                    return;
                }

                fr.lampalon.lifemod.common.service.IConfigurationService config = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.IConfigurationService.class);
                String dateFormat = config.getString("server.date-format", "dd/MM/yyyy HH:mm:ss");
                String expiration = ban.isPermanent() ? "Permanent" : new SimpleDateFormat(dateFormat).format(new Date(ban.getExpirationTime()));
                
                fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                String message = lang.getMessage("sanctions.ban.login",
                        "%reason%", ban.getReason(),
                        "%expiration%", expiration,
                        "%id%", ban.getUuid().toString().substring(0, 8),
                        "%server%", ban.getServerName());

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
            Sanction mute = sanctionService.getActiveSanction(event.getPlayer().getUniqueId(), event.getPlayer().getName(), SanctionType.MUTE).get();
            if (mute != null && !mute.isExpired()) {
                event.setCancelled(true);
                fr.lampalon.lifemod.common.service.IConfigurationService config = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.IConfigurationService.class);
                String dateFormat = config.getString("server.date-format", "dd/MM/yyyy HH:mm:ss");
                String expiration = mute.isPermanent() ? "Jamais" : new SimpleDateFormat(dateFormat).format(new Date(mute.getExpirationTime()));

                fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                String message = lang.getMessage("sanctions.mute.chat-blocked",
                        "%reason%", mute.getReason(),
                        "%expiration%", expiration);

                event.getPlayer().sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

