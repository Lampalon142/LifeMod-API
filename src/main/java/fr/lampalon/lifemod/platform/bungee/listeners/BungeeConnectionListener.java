package fr.lampalon.lifemod.platform.bungee.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class BungeeConnectionListener implements Listener {

    @EventHandler
    public void onLogin(LoginEvent event) {
        UUID uuid = event.getConnection().getUniqueId();
        String name = event.getConnection().getName();
        
        // On enregistre une intention pour forcer Bungee à attendre notre tâche async
        event.registerIntent(BungeeLifeMod.getInstance());

        ProxyServer.getInstance().getScheduler().runAsync(BungeeLifeMod.getInstance(), () -> {
            try {
                ProxyServer.getInstance().getLogger().info("[LifeMod-Debug] Checking ban for " + name + " (" + uuid + ")...");

                ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
                ILangService lang = ServiceRegistry.get(ILangService.class);
                
                Sanction activeBan = sanctionService.getActiveSanction(uuid, name, SanctionType.BAN).join();

                if (activeBan != null) {
                    ProxyServer.getInstance().getLogger().info("[LifeMod-Debug] " + name + " is BANNED. Reason: " + activeBan.getReason());
                    String message = lang.getMessage("sanctions.ban.login");
                    message = message
                            .replace("%reason%", activeBan.getReason())
                            .replace("%issuer%", activeBan.getIssuerName())
                            .replace("%time%", TimeUtil.formatTime(activeBan.getExpirationTime() - System.currentTimeMillis()))
                            .replace("%server%", activeBan.getServerName());
                    
                    event.setCancelled(true);
                    event.setCancelReason(new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
                } else {
                    ProxyServer.getInstance().getLogger().info("[LifeMod-Debug] No ban found for " + name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Release intent
                event.completeIntent(BungeeLifeMod.getInstance());
            }
        });
    }
}
