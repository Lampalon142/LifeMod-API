package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import org.bukkit.entity.Player;
import java.util.Set;
import java.util.UUID;

public interface IVanishService {
    boolean isVanished(UUID uuid);
    void setVanished(Player player, boolean vanished, boolean silent);
    Set<UUID> getVanishedPlayers();
    void updateAllForPlayer(Player player);
}
