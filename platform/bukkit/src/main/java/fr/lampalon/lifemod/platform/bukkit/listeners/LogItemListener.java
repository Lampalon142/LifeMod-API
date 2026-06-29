package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class LogItemListener extends LogBaseListener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!enabled("log-item")) return;
        Player p = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        logAsync(LogEntry.builder()
            .type(LogType.ITEM_DROP).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + item.getType().name() + "\",\"a\":" + item.getAmount() + "}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!enabled("log-item")) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        logAsync(LogEntry.builder()
            .type(LogType.ITEM_PICKUP).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + item.getType().name() + "\",\"a\":" + item.getAmount() + "}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!enabled("log-item")) return;
        Player p = event.getPlayer();
        logAsync(LogEntry.builder()
            .type(LogType.ITEM_CONSUME).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + event.getItem().getType().name() + "\"}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!enabled("log-item")) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();
        logAsync(LogEntry.builder()
            .type(LogType.CRAFT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + result.getType().name() + "\",\"a\":" + result.getAmount() + "}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!enabled("log-item")) return;
        Player p = event.getEnchanter();
        StringBuilder enchants = new StringBuilder();
        event.getEnchantsToAdd().forEach((e, lvl) -> {
            if (enchants.length() > 0) enchants.append(",");
            enchants.append(e.getKey().getKey()).append(":").append(lvl);
        });
        logAsync(LogEntry.builder()
            .type(LogType.ENCHANT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"i\":\"" + event.getItem().getType().name() + "\",\"e\":\"" + enchants + "\"}")
            .world(locData(p.getLocation())).x(p.getLocation().getBlockX()).y(p.getLocation().getBlockY()).z(p.getLocation().getBlockZ())
            .serverName(serverName()).now().build());
    }
}
