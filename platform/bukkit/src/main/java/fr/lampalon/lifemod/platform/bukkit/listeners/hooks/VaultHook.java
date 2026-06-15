package fr.lampalon.lifemod.platform.bukkit.listeners.hooks;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILogService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook implements Listener {

    private final ILogService logService;
    private final IConfigurationService config;
    private boolean vaultDetected;

    public VaultHook() {
        this.logService = ServiceRegistry.get(ILogService.class);
        this.config = ServiceRegistry.get(IConfigurationService.class);
        this.vaultDetected = Bukkit.getPluginManager().getPlugin("Vault") != null;
        if (vaultDetected) {
            Bukkit.getLogger().info("[LifeMod-Logs] Vault detected. Economy transactions will be logged when shop plugins fire events.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (!vaultDetected && event.getProvider().getService().getName().contains("net.milkbowl.vault")) {
            vaultDetected = true;
            Bukkit.getLogger().info("[LifeMod-Logs] Vault economy service registered.");
        }
    }
}
