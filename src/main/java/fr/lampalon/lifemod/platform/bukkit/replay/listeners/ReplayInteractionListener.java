package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.replay.ReplayPlayerManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ReplayInteractionListener implements Listener {
    private final LifeMod plugin;

    public ReplayInteractionListener(LifeMod plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ReplayPlayerManager rpm = plugin.getReplayPlayerManager();

        if (!rpm.isInReplay(player)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        event.setCancelled(true);

        switch (item.getType()) {
            case CLOCK:
                player.sendMessage("§eToggling playback...");
                // Handle pause/resume
                break;
            case ARROW:
                if (item.getItemMeta().getDisplayName().contains("Rewind")) {
                    player.sendMessage("§bRewinding 5 seconds...");
                } else {
                    player.sendMessage("§bFast forwarding 5 seconds...");
                }
                break;
            case BOOK:
                player.sendMessage("§dOpening speed control...");
                // Here you could open a small chat-based speed control or similar, 
                // but since the user doesn't want GUIs, maybe just cycle through speeds.
                break;
            case BARRIER:
                rpm.exitReplay(player);
                player.sendMessage("§aExited replay mode. Your state has been restored.");
                break;
            default:
                break;
        }
    }
}
