package fr.lampalon.lifemod.platform.bukkit.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;

import java.util.UUID;

public class FreezePacketListener implements PacketListener {

    private final LifeMod plugin;

    public FreezePacketListener(LifeMod plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getUser() == null) return;
        UUID uuid = event.getUser().getUUID();
        if (uuid == null) return;
        if (plugin.getFreezeManager() == null) return;
        if (plugin.getFreezeManager().isPlayerFrozen(uuid)) {
            if (isMovementPacket(event.getPacketType()) || isInteractionPacket(event.getPacketType())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isMovementPacket(com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION ||
               type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
               type == PacketType.Play.Client.PLAYER_ROTATION;
    }

    private boolean isInteractionPacket(com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type) {
        return type == PacketType.Play.Client.INTERACT_ENTITY ||
               type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT ||
               type == PacketType.Play.Client.PLAYER_DIGGING ||
               type == PacketType.Play.Client.ANIMATION;
    }
}
