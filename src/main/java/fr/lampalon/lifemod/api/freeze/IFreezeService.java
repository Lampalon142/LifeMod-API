package fr.lampalon.lifemod.api.freeze;

import java.util.Set;
import java.util.UUID;

public interface IFreezeService {

    boolean isFrozen(UUID playerUuid);

    void freeze(UUID moderatorUuid, UUID targetUuid);

    void unfreeze(UUID moderatorUuid, UUID targetUuid);

    Set<UUID> getFrozenPlayers();
}