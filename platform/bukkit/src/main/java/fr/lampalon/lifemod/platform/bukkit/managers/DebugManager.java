package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

public class DebugManager {
    private final LifeMod plugin;
    private boolean cachedEnabled;
    private final java.util.Map<String, Boolean> cachedModules = new java.util.concurrent.ConcurrentHashMap<>();
    private Level cachedLogLevel;
    private String cachedPrefix;

    public DebugManager(LifeMod plugin) {
        this.plugin = plugin;
        if (plugin.getConfigConfig() == null) {
            Bukkit.getLogger().warning("[LifeMod] DebugManager initialized before config loaded!");
        }
        reloadCache();
    }

    public void reloadCache() {
        if (plugin.getConfigConfig() == null) return;
        cachedEnabled = plugin.getConfigConfig().getBoolean("debug.enabled", false);
        cachedModules.clear();
        Level level;
        try {
            String lvl = plugin.getConfigConfig().getString("debug.log-level", "INFO").toUpperCase();
            switch (lvl) {
                case "DEBUG": level = Level.FINE; break;
                case "WARNING": level = Level.WARNING; break;
                case "ERROR": level = Level.SEVERE; break;
                default: level = Level.INFO;
            }
        } catch (Exception e) {
            level = Level.INFO;
        }
        cachedLogLevel = level;
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        cachedPrefix = lang != null ? lang.getMessage("debug.messages.prefix") : "";
    }

    public boolean isEnabled() {
        return cachedEnabled;
    }

    public boolean isModuleEnabled(String module) {
        return cachedModules.computeIfAbsent(module.toLowerCase(),
                m -> plugin.getConfigConfig().getBoolean("debug.modules." + m, false));
    }

    private Level getLogLevel() {
        return cachedLogLevel;
    }

    public void log(String module, String message) {
        if (!cachedEnabled) return;
        if (!isModuleEnabled(module)) return;
        Bukkit.getLogger().log(cachedLogLevel, cachedPrefix + "[" + module.toUpperCase() + "] " + message);
    }

    public void userError(CommandSender sender, String context, Exception e) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        
        String prefix = lang.getMessage("debug.messages.prefix");
        String userMsg = lang.getMessage("debug.messages.user-error", "%context%", context != null ? context : "unknown");
        String helpMsg = lang.getMessage("debug.messages.user-help");

        if (sender != null) {
            sender.sendMessage(prefix + userMsg);
            sender.sendMessage(prefix + helpMsg);

            if (isEnabled() && sender.hasPermission("lifemod.debug") && e != null) {
                sendAdminError(sender, e);
            }
        } else {
            plugin.getLogger().warning(prefix + userMsg);
            plugin.getLogger().warning(prefix + helpMsg);
            if (e != null) e.printStackTrace();
        }
    }

    public void sendAdminError(CommandSender sender, Exception e) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        String adminMsg = lang.getMessage("debug.messages.admin-error",
                "%error%", e.getClass().getSimpleName(),
                "%message%", e.getMessage() == null ? "No message" : e.getMessage());
        sender.sendMessage(adminMsg);

        StackTraceElement top = e.getStackTrace().length > 0 ? e.getStackTrace()[0] : null;
        if (top != null) {
            String locMsg = lang.getMessage("debug.messages.admin-location", "%location%", top.toString());
            sender.sendMessage(locMsg);
        }
    }
}


