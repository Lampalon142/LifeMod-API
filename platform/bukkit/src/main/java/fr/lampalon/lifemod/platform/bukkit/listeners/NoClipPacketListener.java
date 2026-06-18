package fr.lampalon.lifemod.platform.bukkit.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            handleEntityInteract(event, player);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            tempCreative(player);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            tempCreative(player);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!noClipManager.isNoClip(player.getUniqueId())) return;
        if (event.getPacketType() != PacketType.Play.Server.CHANGE_GAME_STATE) return;

        WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);
        if (packet.getReason() != WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) return;

        float originalValue = switch (noClipManager.getOriginalGameMode(player.getUniqueId())) {
            case SURVIVAL  -> 0.0f;
            case CREATIVE  -> 1.0f;
            case ADVENTURE -> 2.0f;
            case SPECTATOR -> 3.0f;
        };

        if (Math.abs(packet.getValue() - originalValue) > 0.01f) {
            event.setCancelled(true);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerChangeGameState(
                    WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE,
                    originalValue));
        }
    }

    private void handleDigging(PacketReceiveEvent event, Player player) {
        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = wrapper.getAction();

        if (action != DiggingAction.FINISHED_DIGGING && action != DiggingAction.START_DIGGING) return;

        Vector3i pos = wrapper.getBlockPosition();
        GameMode originalGm = noClipManager.getOriginalGameMode(player.getUniqueId());

        boolean shouldBreak = (originalGm == GameMode.CREATIVE && action == DiggingAction.START_DIGGING)
                || (originalGm != GameMode.CREATIVE && action == DiggingAction.FINISHED_DIGGING);

        if (!shouldBreak) return;

        event.setCancelled(true);

        org.bukkit.block.Block block = player.getWorld().getBlockAt(pos.x, pos.y, pos.z);
        if (block.isEmpty()) return;

        Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);
        if (player.getLocation().distanceSquared(blockLoc) > 36) return;

        if (originalGm == GameMode.CREATIVE) {
            block.setType(org.bukkit.Material.AIR);
        } else {
            Bukkit.getScheduler().runTask(noClipManager.getPlugin(), () ->
                block.breakNaturally(player.getInventory().getItemInMainHand()));
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
        } else {
            event.setCancelled(true);
            tempCreative(player);
        }
    }

    private void tempCreative(Player player) {
        player.setGameMode(GameMode.CREATIVE);
        Bukkit.getScheduler().runTask(noClipManager.getPlugin(), () ->
            player.setGameMode(GameMode.SPECTATOR));
    }
}
