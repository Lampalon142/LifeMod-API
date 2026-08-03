package fr.lampalon.lifemod.platform.bukkit.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {

            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
            if (wrapper.isOnGround()) {
                wrapper.setOnGround(false);
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            handleDigging(event, player);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!noClipManager.isNoClip(player.getUniqueId())) return;

        if (event.getPacketType() != PacketType.Play.Server.CHANGE_GAME_STATE) return;

        WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);
        if (packet.getReason() != WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) return;
        if (Math.abs(packet.getValue() - 3.0f) > 0.01f) return;

        // The server just switched the player to SPECTATOR (3). Rewrite the
        // packet so the client HUD keeps the original gamemode while the
        // client stays collision-free (no PlayerInfo spoof).
        event.setCancelled(true);
        float originalValue = switch (noClipManager.getOriginalGameMode(player.getUniqueId())) {
            case CREATIVE -> 1.0f;
            case ADVENTURE -> 2.0f;
            case SPECTATOR -> 3.0f;
            default -> 0.0f;
        };
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerChangeGameState(
                        WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE, originalValue));
    }

    private void handleDigging(PacketReceiveEvent event, Player player) {
        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = wrapper.getAction();

        if (action != DiggingAction.START_DIGGING && action != DiggingAction.FINISHED_DIGGING) return;

        GameMode originalGm = noClipManager.getOriginalGameMode(player.getUniqueId());
        boolean shouldBreak = (originalGm == GameMode.CREATIVE && action == DiggingAction.START_DIGGING)
                || (originalGm != GameMode.CREATIVE && action == DiggingAction.FINISHED_DIGGING);
        if (!shouldBreak) return;

        event.setCancelled(true);
        Vector3i pos = wrapper.getBlockPosition();

        Bukkit.getScheduler().runTask(noClipManager.getPlugin(), () -> {
            Block block = player.getWorld().getBlockAt(pos.x, pos.y, pos.z);
            if (block.isEmpty() || block.isLiquid()) return;
            if (player.getLocation().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) > 36) return;

            if (originalGm == GameMode.CREATIVE) {
                block.setType(Material.AIR);
            } else {
                block.breakNaturally(player.getInventory().getItemInMainHand());
            }
        });
    }
}