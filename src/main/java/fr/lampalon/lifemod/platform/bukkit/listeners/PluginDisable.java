package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

public class PluginDisable implements Listener {
    private final DebugManager debug = LifeMod.getInstance().getDebugManager();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginDisable(PluginDisableEvent event) {
        if (!event.getPlugin().equals(LifeMod.getInstance())) return;

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (LifeMod.getInstance().getStaffModeManager().isMod(p)) {
                LifeMod.getInstance().getStaffModeManager().disableStaffMode(p);
                debug.log("mod", p.getName() + " moderation mode disabled on plugin shutdown.");
            }
        });
    }
}
