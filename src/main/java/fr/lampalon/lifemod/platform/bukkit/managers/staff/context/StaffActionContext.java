package fr.lampalon.lifemod.platform.bukkit.managers.staff.context;

import fr.lampalon.lifemod.platform.bukkit.managers.staff.model.StaffItem;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class StaffActionContext {

    private final Player player;
    private final StaffItem staffItem;
    private final String clickType;

    private final PlayerInteractEvent interactEvent;
    private final PlayerInteractEntityEvent entityInteractEvent;
    private final Entity targetEntity;
    private final Block clickedBlock;

    public StaffActionContext(Player player, StaffItem staffItem, String clickType,
                              PlayerInteractEvent interactEvent,
                              PlayerInteractEntityEvent entityInteractEvent,
                              Entity targetEntity) {
        this.player = player;
        this.staffItem = staffItem;
        this.clickType = clickType;
        this.interactEvent = interactEvent;
        this.entityInteractEvent = entityInteractEvent;
        this.targetEntity = targetEntity;
        this.clickedBlock = interactEvent != null ? interactEvent.getClickedBlock() : null;
    }

    public Player getPlayer() { return player; }
    public StaffItem getStaffItem() { return staffItem; }
    public String getClickType() { return clickType; }
    public PlayerInteractEvent getInteractEvent() { return interactEvent; }
    public PlayerInteractEntityEvent getEntityInteractEvent() { return entityInteractEvent; }
    public Entity getTargetEntity() { return targetEntity; }
    public Block getClickedBlock() { return clickedBlock; }

    public boolean hasEntityTarget() { return targetEntity != null; }
    public boolean hasBlockTarget() { return clickedBlock != null; }
    public boolean isEntityInteraction() { return entityInteractEvent != null; }
}
