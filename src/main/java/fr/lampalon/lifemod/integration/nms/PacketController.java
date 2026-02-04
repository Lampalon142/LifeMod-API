package fr.lampalon.lifemod.integration.nms;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PacketController implements PacketListener {

    private final LifeMod plugin;

    public PacketController(LifeMod plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getUser() == null) return;
        UUID uuid = event.getUser().getUUID();
        if (plugin.getFreezeManager().isPlayerFrozen(uuid)) {
            if (isMovementPacket(event.getPacketType())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isMovementPacket(com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION ||
               type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
               type == PacketType.Play.Client.PLAYER_ROTATION;
    }

    public void sendActionBar(Player player, String message) {
        WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(Component.text(message));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}