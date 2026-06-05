package fr.lampalon.lifemod.platform.bungee.listeners;

import fr.lampalon.lifemod.common.antivpn.AntiVPNService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bungee.BungeeLifeMod;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetSocketAddress;

public class BungeeAntiVPNListener implements Listener {

    private final BungeeLifeMod plugin;
    private final AntiVPNService antiVPNService;

    public BungeeAntiVPNListener(BungeeLifeMod plugin, AntiVPNService antiVPNService) {
        this.plugin = plugin;
        this.antiVPNService = antiVPNService;
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (event.isCancelled()) return;

        String name = event.getConnection().getName();
        if (name == null) return;

        String ip = ((InetSocketAddress) event.getConnection().getSocketAddress()).getAddress().getHostAddress();

        event.registerIntent(plugin);

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                boolean allowed = antiVPNService.shouldAllowConnection(ip, name).join();
                if (!allowed) {
                    ILangService lang = ServiceRegistry.get(ILangService.class);
                    String reason = lang != null ? lang.getMessage("antivpn.kick-reason", "vpn.anti-vpn.default") : "vpn.anti-vpn.default";
                    event.setCancelled(true);
                    event.setCancelReason(new TextComponent(reason));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                event.completeIntent(plugin);
            }
        });
    }
}
