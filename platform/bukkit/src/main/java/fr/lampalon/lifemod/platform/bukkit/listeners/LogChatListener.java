package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class LogChatListener extends LogBaseListener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled("log-chat")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.CHAT_MESSAGE).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"m\":\"" + jsonEscape(event.getMessage()) + "\"}")
            .serverName(serverName()).now().build());
    }
}
