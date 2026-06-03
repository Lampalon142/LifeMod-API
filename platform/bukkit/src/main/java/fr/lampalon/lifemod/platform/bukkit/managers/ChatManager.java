package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatManager implements Listener {
    private final LifeMod plugin;
    private final DebugManager debug;
    private boolean enabled;
    private List<String> blacklist;
    private final java.util.Map<java.util.UUID, java.util.function.Consumer<String>> inputCallbacks = new java.util.HashMap<>();

    public ChatManager(LifeMod plugin) {
        this.plugin = plugin;
        this.debug = plugin.getDebugManager();
        reloadConfig();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void awaitChatInput(Player player, java.util.function.Consumer<String> callback) {
        inputCallbacks.put(player.getUniqueId(), callback);
    }

    public void reloadConfig() {
        enabled = plugin.getConfig().getBoolean("modules.chat-manager.enabled", true);
        blacklist = plugin.getConfig().getStringList("modules.chat-manager.blacklist");
        debug.log("chat", "ChatManager configuration reloaded. Enabled: " + enabled + ", Blacklist: " + blacklist);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (inputCallbacks.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            java.util.function.Consumer<String> callback = inputCallbacks.remove(event.getPlayer().getUniqueId());
            // Run on next tick to avoid async issues if callback opens GUI
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(event.getMessage()));
            return;
        }

        if (enabled) {
            String message = event.getMessage();
            Player player = event.getPlayer();
            if (!player.hasPermission("lifemod.chat.bypass")) {
                for (String word : blacklist) {
                    if (message.toLowerCase().contains(word.toLowerCase())) {
                        event.setMessage(filterWord(message, word));
                        notifyViewers(player, word);
                        trackChatFilter();
                        debug.log("chat", player.getName() + " used blacklisted word: " + word);
                        break;
                    }
                }
            }
        }
    }

    private String filterWord(String message, String word) {
        return message.replaceAll("(?i)" + word, "***");
    }

    private void notifyViewers(Player sender, String word) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("lifemod.chat.views")) {
                String notification = lang.getMessage("commands.chat.forbidden-word",
                        "%player%", sender.getName(),
                        "%word%", word);
                player.sendMessage(notification);
            }
        }
    }

    private void trackChatFilter() {
        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("action", "blocked");
            ph.capture("lifemod_chat_filter", props);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("modules.chat-manager.enabled", enabled);
        plugin.saveConfig();
        debug.log("chat", "ChatManager enabled set to " + enabled);
    }

    public void addToBlacklist(String word) {
        blacklist.add(word);
        plugin.getConfig().set("modules.chat-manager.blacklist", blacklist);
        plugin.saveConfig();
        debug.log("chat", "Added word to blacklist: " + word);
    }

    public void removeFromBlacklist(String word) {
        blacklist.remove(word);
        plugin.getConfig().set("modules.chat-manager.blacklist", blacklist);
        plugin.saveConfig();
        debug.log("chat", "Removed word from blacklist: " + word);
    }
}


