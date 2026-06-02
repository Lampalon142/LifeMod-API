package fr.lampalon.lifemod.platform.bukkit.events.vanish;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerVanishStateChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final boolean vanished;
    private final boolean silent;

    public PlayerVanishStateChangeEvent(Player player, boolean vanished, boolean silent) {
        this.player = player;
        this.vanished = vanished;
        this.silent = silent;
    }

    public Player getPlayer() { return player; }
    public boolean isVanished() { return vanished; }
    public boolean isSilent() { return silent; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
