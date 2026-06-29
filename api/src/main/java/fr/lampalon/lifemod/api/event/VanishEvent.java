package fr.lampalon.lifemod.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class VanishEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final boolean vanishing;
    private boolean cancelled;

    public VanishEvent(UUID playerUuid, boolean vanishing) {
        this.playerUuid = playerUuid;
        this.vanishing = vanishing;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isVanishing() {
        return vanishing;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
