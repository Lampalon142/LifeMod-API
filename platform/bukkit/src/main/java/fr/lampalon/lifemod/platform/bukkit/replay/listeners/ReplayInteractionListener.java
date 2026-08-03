package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.replay.ReplayPlayerManager;
import org.bukkit.ChatColor;
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
        ILangService lang = ServiceRegistry.get(ILangService.class);

        switch (item.getType()) {
            case CLOCK:
                boolean paused = !pm.isPaused();
                pm.setPaused(paused);
                player.sendMessage(lang.getMessage(paused ? "replay.interaction.paused" : "replay.interaction.resumed"));
                break;
            case ARROW: {
                String stripped = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                String rewindName = ChatColor.stripColor(lang.getMessage("replay.items.rewind"));
                String forwardName = ChatColor.stripColor(lang.getMessage("replay.items.forward"));
                if (stripped.equals(rewindName)) {
                    pm.seekTo(player, pm.getCurrentIndex() - 100);
                    player.sendMessage(lang.getMessage("replay.interaction.rewind"));
                } else if (stripped.equals(forwardName)) {
                    pm.seekTo(player, pm.getCurrentIndex() + 100);
                    player.sendMessage(lang.getMessage("replay.interaction.forward"));
                }
                break;
            }
            case BOOK:
                player.sendMessage(lang.getMessage("replay.interaction.speed-not-implemented"));
                break;
            case BARRIER:
                rpm.exitReplay(player);
                player.sendMessage(lang.getMessage("replay.interaction.exit"));
                break;
            default:
                break;
        }
    }
}
