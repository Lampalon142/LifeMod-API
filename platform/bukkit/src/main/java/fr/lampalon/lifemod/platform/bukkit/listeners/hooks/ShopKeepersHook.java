package fr.lampalon.lifemod.platform.bukkit.listeners.hooks;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILogService;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class ShopKeepersHook implements Listener {

    private final ILogService logService;
    private final IConfigurationService config;

    public ShopKeepersHook() {
        this.logService = ServiceRegistry.get(ILogService.class);
        this.config = ServiceRegistry.get(IConfigurationService.class);
        if (Bukkit.getPluginManager().getPlugin("ShopKeepers") != null) {
            Bukkit.getLogger().info("[LifeMod-Logs] ShopKeepers detected. NPC shop trades will be logged via the public API when available.");
        }
    }
}
