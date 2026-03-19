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
 */
public class ReplayPlayerManager {
    private final LifeMod plugin;
    private final Map<UUID, ReplayPlayerState> savedStates = new HashMap<>();

    public ReplayPlayerManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    /**
     * Prepares a player for replay mode.
     */
    public void enterReplay(Player player) {
        savedStates.put(player.getUniqueId(), new ReplayPlayerState(player));
        
        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE);
        
        giveReplayItems(player);
    }

    /**
     * Restores a player to their state before replay.
     */
    public void exitReplay(Player player) {
        ReplayPlayerState state = savedStates.remove(player.getUniqueId());
        if (state != null) {
            state.restore(player);
        }
    }

    public boolean isInReplay(Player player) {
        return savedStates.containsKey(player.getUniqueId());
    }

    private void giveReplayItems(Player player) {
        ItemStack pauseResume = new ItemBuilder(Material.CLOCK)
                .setName("§ePause / Resume")
                .toItemStack();
        
        ItemStack rewind = new ItemBuilder(Material.ARROW)
                .setName("§bRewind 5s")
                .toItemStack();
        
        ItemStack fastForward = new ItemBuilder(Material.ARROW)
                .setName("§bFast Forward 5s")
                .toItemStack();
        
        ItemStack speedControl = new ItemBuilder(Material.BOOK)
                .setName("§dPlayback Speed")
                .toItemStack();
        
        ItemStack exitReplay = new ItemBuilder(Material.BARRIER)
                .setName("§cExit Replay")
                .toItemStack();

        player.getInventory().setItem(0, rewind);
        player.getInventory().setItem(2, pauseResume);
        player.getInventory().setItem(4, fastForward);
        player.getInventory().setItem(6, speedControl);
        player.getInventory().setItem(8, exitReplay);
        
        player.getInventory().setHeldItemSlot(2);
    }
}
