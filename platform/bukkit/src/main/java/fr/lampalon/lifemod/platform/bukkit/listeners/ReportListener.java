package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class ReportListener implements Listener {

    public ReportListener(LifeMod plugin) {
        IMessagingService msg = ServiceRegistry.get(IMessagingService.class);
        if (msg != null) {
            msg.subscribe("lifemod:reports", message -> {
                String action = extractJsonValue(message, "action");
                if ("CREATE".equals(action)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String reporter = extractJsonValue(message, "reporter");
                        String target = extractJsonValue(message, "target");
                        String reason = extractJsonValue(message, "reason");
                        String server = extractJsonValue(message, "server");
                        String id = extractJsonValue(message, "id");

                        ILangService lang = ServiceRegistry.get(ILangService.class);
                        String notifyMsg = lang.getMessage("reports.staff-notify",
                                "%player%", reporter,
                                "%target%", target,
                                "%reason%", reason,
                                "%server%", server);

                        for (Player online : Bukkit.getOnlinePlayers()) {
                            if (online.hasPermission("lifemod.report.notify")) {
                                TextComponent msgComp = new TextComponent(notifyMsg);
                                msgComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports"));
                                online.spigot().sendMessage(msgComp);
                            }
                        }
                    });
                }
            });
        }
    }

    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start == -1) return "";
            start += search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        start += search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }
}
