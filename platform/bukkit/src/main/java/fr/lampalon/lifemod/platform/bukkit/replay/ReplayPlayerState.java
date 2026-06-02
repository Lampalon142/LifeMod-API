package fr.lampalon.lifemod.platform.bukkit.replay;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Stores a player's state before they enter a replay.
 */
public class ReplayPlayerState {
    private final GameMode gameMode;
    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final Location location;

    public ReplayPlayerState(Player player) {
        this.gameMode = player.getGameMode();
        this.inventory = player.getInventory().getContents().clone();
        this.armor = player.getInventory().getArmorContents().clone();
        this.location = player.getLocation().clone();
    }

    /**
     * Restores the stored state to the player.
     */
    public void restore(Player player) {
        player.setGameMode(gameMode);
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.teleport(location);
    }
}
