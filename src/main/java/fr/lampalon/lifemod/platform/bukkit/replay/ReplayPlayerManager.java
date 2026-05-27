package fr.lampalon.lifemod.platform.bukkit.replay;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.ItemBuilder;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages players currently viewing a replay.
 * The moderator is in CREATIVE mode so they can fly freely around the NPC.
 */
public class ReplayPlayerManager {
    private final LifeMod plugin;
    private final Map<UUID, ReplayPlayerState> savedStates = new HashMap<>();
    private final Map<UUID, PlaybackManager> activePlaybacks = new HashMap<>();

    public ReplayPlayerManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    /**
     * Prepares a player for replay mode.
     * CREATIVE = can fly freely and move around the NPC independently.
     */
    public void enterReplay(Player player) {
        savedStates.put(player.getUniqueId(), new ReplayPlayerState(player));

        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);

        // Hide all real players to isolate the moderator
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                player.hidePlayer(plugin, online);
            }
        }

        giveReplayItems(player);
    }

    /**
     * Restores a player to their state before replay.
     * Also cancels the active playback if any.
     */
    public void exitReplay(Player player) {
        // Stop the playback loop cleanly first
        PlaybackManager pm = activePlaybacks.remove(player.getUniqueId());
        if (pm != null) {
            pm.stopPlayback();
        }

        // Show players again
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                player.showPlayer(plugin, online);
            }
        }

        ReplayPlayerState state = savedStates.remove(player.getUniqueId());
        if (state != null) {
            state.restore(player);
        }
    }

    public void handleQuit(UUID playerId) {
        savedStates.remove(playerId);
        PlaybackManager pm = activePlaybacks.remove(playerId);
        if (pm != null) {
            pm.stopPlayback();
        }
    }

    public boolean isInReplay(Player player) {
        return savedStates.containsKey(player.getUniqueId());
    }

    /**
     * Registers the active PlaybackManager for a player so it can be stopped on /replay stop.
     */
    public void registerPlayback(Player player, PlaybackManager pm) {
        activePlaybacks.put(player.getUniqueId(), pm);
    }

    public PlaybackManager getPlaybackManager(Player player) {
        return activePlaybacks.get(player.getUniqueId());
    }

    private void giveReplayItems(Player player) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        
        ItemStack rewind = new ItemBuilder(Material.ARROW)
                .setName(lang.getMessage("replay.items.rewind"))
                .toItemStack();

        ItemStack forward = new ItemBuilder(Material.ARROW)
                .setName(lang.getMessage("replay.items.forward"))
                .toItemStack();

        ItemStack pause = new ItemBuilder(Material.CLOCK)
                .setName(lang.getMessage("replay.items.pause"))
                .toItemStack();

        ItemStack exitReplay = new ItemBuilder(Material.BARRIER)
                .setName(lang.getMessage("replay.items.exit"))
                .toItemStack();

        player.getInventory().setItem(0, rewind);
        player.getInventory().setItem(1, forward);
        player.getInventory().setItem(4, pause);
        player.getInventory().setItem(8, exitReplay);
        player.getInventory().setHeldItemSlot(4);
    }
}