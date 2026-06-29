package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.gui.PinGui;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorAuthService;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorSessionManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModeratorAuthListener implements Listener {

    private final Map<UUID, PinGui> activeGuis = new HashMap<>();

    private boolean needsAuth(Player player) {
        boolean hasPerm = player.hasPermission("lifemod.moderator");
        boolean isAuth = LifeMod.getInstance().getModeratorSessionManager().isAuthenticated(player.getUniqueId());
        return hasPerm && !isAuth;
    }

    private boolean isAuthGui(String title) {
        if (title == null) return false;
        ILangService lang = ServiceRegistry.get(ILangService.class);
        String registerTitle = ChatColor.stripColor(lang.getMessage("auth.gui.title-register"));
        String loginTitle = ChatColor.stripColor(lang.getMessage("auth.gui.title-login"));
        String stripped = ChatColor.stripColor(title);
        return stripped.equalsIgnoreCase(registerTitle) || stripped.equalsIgnoreCase(loginTitle);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (LifeMod.getInstance().getModeratorSessionManager().isLocked(player.getUniqueId())) {
            ILangService lang = ServiceRegistry.get(ILangService.class);
            player.kickPlayer(lang.getMessage("auth.login-locked"));
            return;
        }
        if (!player.hasPermission("lifemod.moderator")) return;

        ModeratorSessionManager sessionManager = LifeMod.getInstance().getModeratorSessionManager();
        ModeratorAuthService authService = LifeMod.getInstance().getModeratorAuthService();

        // Already authenticated this in-memory session
        if (sessionManager.isAuthenticated(player.getUniqueId())) return;

        // Try to restore session from DB (same IP + within timeout)
        String currentIp = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
        String lastIp = authService.getLastAuthIp(player.getUniqueId());
        long lastTime = authService.getLastAuthTime(player.getUniqueId());
        long timeoutMs = 5L * 60L * 1000L; // 5 minutes

        if (currentIp != null && currentIp.equals(lastIp) && lastTime > 0 && (System.currentTimeMillis() - lastTime) < timeoutMs) {
            sessionManager.authenticate(player.getUniqueId());
            return;
        }

        // Otherwise ask for PIN
        Bukkit.getScheduler().runTaskLater(LifeMod.getInstance(), () -> {
            if (player.isOnline() && needsAuth(player)) {
                PinGui gui = new PinGui(player);
                activeGuis.put(player.getUniqueId(), gui);
                gui.open();
            }
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeGuis.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (needsAuth(player)) {
            Bukkit.getScheduler().runTaskLater(LifeMod.getInstance(), () -> {
                if (player.isOnline() && needsAuth(player)) {
                    PinGui gui = new PinGui(player);
                    activeGuis.put(player.getUniqueId(), gui);
                    gui.open();
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (needsAuth(player)) {
            String title = event.getView().getTitle();
            if (isAuthGui(title)) {
                event.setCancelled(true);
                PinGui gui = activeGuis.get(player.getUniqueId());
                if (gui != null) {
                    gui.handleClick(event.getRawSlot());
                }
                return;
            }
            ILangService lang = ServiceRegistry.get(ILangService.class);
            player.sendMessage(lang.getMessage("auth.blocked-action"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (needsAuth(player)) {
            String title = event.getView().getTitle();
            if (isAuthGui(title)) {
                return;
            }
            ILangService lang = ServiceRegistry.get(ILangService.class);
            player.sendMessage(lang.getMessage("auth.blocked-action"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage().toLowerCase();
        if (needsAuth(player)
                && !(msg.startsWith("/auth login") || msg.startsWith("/auth register"))) {
            ILangService lang = ServiceRegistry.get(ILangService.class);
            player.sendMessage(lang.getMessage("auth.login-required"));
            // Open new gui if not already
            if (player.getOpenInventory().getTopInventory() == null || !isAuthGui(player.getOpenInventory().getTitle())) {
                PinGui gui = new PinGui(player);
                activeGuis.put(player.getUniqueId(), gui);
                gui.open();
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (needsAuth(player)) {
            ILangService lang = ServiceRegistry.get(ILangService.class);
            player.sendMessage(lang.getMessage("auth.blocked-chat"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (needsAuth(player)) {
            ILangService lang = ServiceRegistry.get(ILangService.class);
            player.sendMessage(lang.getMessage("auth.blocked-action"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (needsAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        if (needsAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (needsAuth(player)) {
            event.setKeepInventory(true);
        }
    }
}
