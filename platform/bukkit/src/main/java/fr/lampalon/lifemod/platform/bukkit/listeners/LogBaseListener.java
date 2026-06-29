package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILogService;
import org.bukkit.Location;
import org.bukkit.event.Listener;

public abstract class LogBaseListener implements Listener {

    protected final ILogService logService;
    protected final IConfigurationService config;

    protected LogBaseListener() {
        this.logService = ServiceRegistry.get(ILogService.class);
        this.config = ServiceRegistry.get(IConfigurationService.class);
    }

    protected boolean enabled(String path) {
        return config.getBoolean("logs." + path, true);
    }

    protected void logAsync(LogEntry entry) {
        if (logService != null) logService.log(entry);
    }

    protected static String locData(Location loc) {
        if (loc == null) return null;
        return loc.getWorld() != null ? loc.getWorld().getName() : null;
    }

    protected String serverName() {
        return config.getString("server.name", "unknown");
    }

    protected static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
