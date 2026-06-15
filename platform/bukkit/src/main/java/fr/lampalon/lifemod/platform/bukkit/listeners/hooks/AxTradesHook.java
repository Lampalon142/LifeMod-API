package fr.lampalon.lifemod.platform.bukkit.listeners.hooks;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILogService;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class AxTradesHook implements Listener {

    private final ILogService logService;
    private final IConfigurationService config;

    public AxTradesHook() {
        this.logService = ServiceRegistry.get(ILogService.class);
        this.config = ServiceRegistry.get(IConfigurationService.class);
        if (Bukkit.getPluginManager().getPlugin("AxTrades") != null) {
            Bukkit.getLogger().info("[LifeMod-Logs] AxTrades detected. Player trades will be logged via the public API when available.");
        }
    }
}
