package fr.lampalon.lifemod.integration.nms;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTitle;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
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
        WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(Component.text(MessageUtil.formatMessage(message)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        // Send times
        WrapperPlayServerTitle timePacket = new WrapperPlayServerTitle(WrapperPlayServerTitle.Action.TIMES, fadeIn, stay, fadeOut);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, timePacket);

        // Send title
        if (title != null) {
            WrapperPlayServerTitle titlePacket = new WrapperPlayServerTitle(WrapperPlayServerTitle.Action.TITLE, Component.text(MessageUtil.formatMessage(title)));
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, titlePacket);
        }

        // Send subtitle
        if (subtitle != null) {
            WrapperPlayServerTitle subtitlePacket = new WrapperPlayServerTitle(WrapperPlayServerTitle.Action.SUBTITLE, Component.text(MessageUtil.formatMessage(subtitle)));
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, subtitlePacket);
        }
    }

    public void kickPlayer(Player player, String reason) {
        WrapperPlayServerDisconnect packet = new WrapperPlayServerDisconnect(Component.text(MessageUtil.formatMessage(reason)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}