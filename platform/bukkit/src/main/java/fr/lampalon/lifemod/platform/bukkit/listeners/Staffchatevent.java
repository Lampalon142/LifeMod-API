package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class Staffchatevent implements Listener {
    private final LifeMod plugin;
    private final DebugManager debug;

    public Staffchatevent(LifeMod plugin) {
        this.plugin = plugin;
        this.debug = plugin.getDebugManager();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        if (!config.getBoolean("modules.staffchat.enabled", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("lifemod.staffchat")) return;

        String prefix = config.getString("modules.staffchat.prefix", "!");
        String message = event.getMessage();

        if (!message.startsWith(prefix)) return;

        event.setCancelled(true);
        String rawMsg = message.substring(prefix.length());

        ILangService lang = ServiceRegistry.get(ILangService.class);
        String staffMessage = lang.getMessage("commands.staffchat.message",
                "%player%", player.getName(),
                "%message%", rawMsg);

        for (Player recipient : plugin.getServer().getOnlinePlayers()) {
            if (recipient.hasPermission("lifemod.staffchat")) {
                recipient.sendMessage(staffMessage);
            }
        }
        debug.log("staffchat", player.getName() + " sent staffchat message: " + rawMsg);

        IMessagingService messaging = ServiceRegistry.get(IMessagingService.class);
        if (messaging != null) {
            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
            messaging.publish("lifemod:staff", "CHAT|" + player.getUniqueId() + "|" + player.getName() + "|" + rawMsg + "|" + platform.getServerName());
        }
    }
}


