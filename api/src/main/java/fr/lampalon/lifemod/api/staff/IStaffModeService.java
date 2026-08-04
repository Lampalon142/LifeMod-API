package fr.lampalon.lifemod.api.staff;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public interface IStaffModeService {

    boolean isInStaffMode(UUID playerUuid);

    void enable(Player player);

    void disable(Player player);

    Set<UUID> getStaffPlayers();
}