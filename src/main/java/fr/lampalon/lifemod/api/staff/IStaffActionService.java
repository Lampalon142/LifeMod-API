package fr.lampalon.lifemod.api.staff;

import org.bukkit.entity.Player;

import java.util.Set;

public interface IStaffActionService {

    Set<StaffActionType> getAvailableActions();

    boolean requiresTarget(StaffActionType type);

    /**
     * Executes a native staff action (freeze, vanish, jump, inspect…) as if the
     * moderator had triggered it via their staff item.
     *
     * @return {@code false} if the action type is not registered or the target is missing
     */
    boolean execute(Player moderator, Player target, StaffActionType type);
}