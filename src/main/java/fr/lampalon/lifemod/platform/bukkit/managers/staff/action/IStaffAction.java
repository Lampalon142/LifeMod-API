package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public interface IStaffAction {
    /**
     * Exécuté lors d'un clic sur un bloc ou dans l'air.
     */
    void onInteract(Player player, PlayerInteractEvent event);

    /**
     * Exécuté lors d'un clic sur une entité (joueur ou mob).
     */
    void onInteractEntity(Player player, PlayerInteractEntityEvent event);

    /**
     * Exécuté lors d'une attaque (Clic Gauche sur entité).
     */
    default void onAttack(Player player, org.bukkit.entity.Entity target) {}
}
