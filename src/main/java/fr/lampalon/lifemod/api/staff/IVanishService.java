package fr.lampalon.lifemod.api.staff;

import java.util.Set;
import java.util.UUID;

public interface IVanishService {

    boolean isVanished(UUID uuid);

    void setVanished(UUID uuid, boolean vanished, boolean silent);

    Set<UUID> getVanishedPlayers();
}
