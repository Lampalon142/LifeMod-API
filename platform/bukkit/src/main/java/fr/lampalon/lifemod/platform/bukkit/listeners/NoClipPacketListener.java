package fr.lampalon.lifemod.platform.bukkit.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public class NoClipPacketListener extends PacketListenerAbstract {

    private final NoClipManager noClipManager;

    public NoClipPacketListener(NoClipManager noClipManager) {
        super(PacketListenerPriority.HIGHEST);
        this.noClipManager = noClipManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!noClipManager.isNoClip(player.getUniqueId())) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            handleDigging(event, player);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            handlePlacement(event, player);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            handleEntityInteract(event, player);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!noClipManager.isNoClip(player.getUniqueId())) return;

        if (event.getPacketType() == PacketType.Play.Server.CHANGE_GAME_STATE) {
            WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);
            if (packet.getReason() != WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) return;

            // Keep the client thinking it is in CREATIVE (spoofed)
            if (Math.abs(packet.getValue() - 1.0f) > 0.01f) {
                event.setCancelled(true);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                        new WrapperPlayServerChangeGameState(
                                WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE, 1.0f));
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.PLAYER_ABILITIES) {
            WrapperPlayServerPlayerAbilities abilities = new WrapperPlayServerPlayerAbilities(event);
            abilities.setFlying(true);
            abilities.setFlightAllowed(true);
            abilities.setInCreativeMode(true);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
            if (!wrapper.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE)) return;

            List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> entries = new ArrayList<>(wrapper.getEntries());
            boolean changed = false;
            for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : entries) {
                if (entry.getGameMode() != GameMode.CREATIVE) {
                    entry.setGameMode(GameMode.CREATIVE);
                    changed = true;
                }
            }

            if (changed) {
                wrapper.setEntries(entries);
            }
        }
    }

    private void handleDigging(PacketReceiveEvent event, Player player) {
        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = wrapper.getAction();

        if (action != DiggingAction.START_DIGGING && action != DiggingAction.FINISHED_DIGGING) return;

        event.setCancelled(true);
        Vector3i pos = wrapper.getBlockPosition();

        Bukkit.getScheduler().runTask(noClipManager.getPlugin(), () -> {
            Block block = player.getWorld().getBlockAt(pos.x, pos.y, pos.z);
            if (block.isEmpty() || block.isLiquid()) return;
            if (!inReach(player, block.getLocation().add(0.5, 0.5, 0.5))) return;

            if (noClipManager.getOriginalGameMode(player.getUniqueId()) == org.bukkit.GameMode.CREATIVE) {
                block.setType(Material.AIR);
            } else {
                block.breakNaturally(player.getInventory().getItemInMainHand());
            }

            sendBlockChange(player, block);
        });
    }

    private void handlePlacement(PacketReceiveEvent event, Player player) {
        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        event.setCancelled(true);

        Vector3i clicked = wrapper.getBlockPosition();
        if (wrapper.getFace() == null || wrapper.getFace() == com.github.retrooper.packetevents.protocol.world.BlockFace.OTHER) {
            return;
        }
        Vector3i target = clicked.offset(wrapper.getFace());

        Bukkit.getScheduler().runTask(noClipManager.getPlugin(), () ->
                handlePlacementSync(player, clicked, target));
    }

    private void handlePlacementSync(Player player, Vector3i clickedPos, Vector3i targetPos) {
        Block clicked = player.getWorld().getBlockAt(clickedPos.x, clickedPos.y, clickedPos.z);
        BlockState clickedState = clicked.getState();

        if (clickedState instanceof InventoryHolder holder) {
            player.openInventory(holder.getInventory());
            ILangService lang = ServiceRegistry.get(ILangService.class);
            if (lang != null) {
                player.sendMessage(lang.getMessage("commands.noclip.chest-opened"));
            }
            return;
        }

        Block target = player.getWorld().getBlockAt(targetPos.x, targetPos.y, targetPos.z);
        if (!target.isPassable()) return;
        if (!inReach(player, target.getLocation().add(0.5, 0.5, 0.5))) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir() || !held.getType().isBlock()) return;

        if (hasEntityInside(target)) return;

        target.setBlockData(held.getType().createBlockData(), true);
        sendBlockChange(player, target);
        sendBlockChange(player, clicked);

        if (noClipManager.getOriginalGameMode(player.getUniqueId()) != org.bukkit.GameMode.CREATIVE) {
            int amount = held.getAmount() - 1;
            if (amount > 0) {
                held.setAmount(amount);
                player.getInventory().setItemInMainHand(held);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    private void handleEntityInteract(PacketReceiveEvent event, Player player) {
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        Entity target = null;
        for (Entity e : player.getWorld().getEntities()) {
            if (e.getEntityId() == wrapper.getEntityId()) {
                target = e;
                break;
            }
        }
        if (target == null) return;

        if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            event.setCancelled(true);
            Entity finalTarget = target;
            Bukkit.getScheduler().runTask(noClipManager.getPlugin(), () ->
                    player.attack(finalTarget));
        }
    }

    private boolean inReach(Player player, Location location) {
        double reach = noClipManager.reach();
        return player.getEyeLocation().distanceSquared(location) <= reach * reach;
    }

    private boolean hasEntityInside(Block block) {
        BoundingBox box = BoundingBox.of(block.getLocation(), block.getLocation().add(1, 1, 1));
        return !block.getWorld().getNearbyEntities(box).isEmpty();
    }

    private void sendBlockChange(Player player, Block block) {
        try {
            int blockId = SpigotConversionUtil.fromBukkitBlockData(block.getBlockData()).getGlobalId();
            PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                    new WrapperPlayServerBlockChange(
                            new Vector3i(block.getX(), block.getY(), block.getZ()), blockId));
        } catch (Exception ignored) {
        }
    }
}
