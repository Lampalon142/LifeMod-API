package fr.lampalon.lifemod.api.event;

import fr.lampalon.lifemod.api.sanction.Sanction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SanctionEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Sanction sanction;
    private boolean cancelled;

    public SanctionEvent(Sanction sanction) {
        this.sanction = sanction;
    }

    public Sanction getSanction() {
        return sanction;
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
