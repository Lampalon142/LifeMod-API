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

        fr.lampalon.lifemod.platform.bukkit.replay.PlaybackManager pm = rpm.getPlaybackManager(player);
        if (pm == null) return;

        event.setCancelled(true);
        String name = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "";

        switch (item.getType()) {
            case CLOCK:
                boolean paused = !pm.isPaused();
                pm.setPaused(paused);
                player.sendMessage(paused ? "§eReplay mis en pause." : "§aReprise du replay.");
                break;
            case ARROW:
                if (name.contains("Reculer")) {
                    pm.seekTo(player, pm.getCurrentIndex() - 100);
                    player.sendMessage("§bRecul de 5 secondes...");
                } else {
                    pm.seekTo(player, pm.getCurrentIndex() + 100);
                    player.sendMessage("§bAvance de 5 secondes...");
                }
                break;
            case BOOK:
                player.sendMessage("§dContrôle de vitesse non implémenté.");
                break;
            case BARRIER:
                rpm.exitReplay(player);
                player.sendMessage("§aSortie du mode replay. Votre état a été restauré.");
                break;
            default:
                break;
        }
    }
}
