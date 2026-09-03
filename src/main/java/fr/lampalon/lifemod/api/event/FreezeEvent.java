package fr.lampalon.lifemod.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class FreezeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID targetUuid;
    private final UUID moderatorUuid;
    private final boolean frozen;
    private boolean cancelled;

    public FreezeEvent(UUID targetUuid, UUID moderatorUuid, boolean frozen) {
        this.targetUuid = targetUuid;
        this.moderatorUuid = moderatorUuid;
        this.frozen = frozen;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public UUID getModeratorUuid() {
        return moderatorUuid;
    }

    public boolean isFrozen() {
        return frozen;
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
