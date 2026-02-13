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

    public void sendActionBar(Player player, String message) {
        WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(Component.text(MessageUtil.formatMessage(message)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComp = title != null ? Component.text(MessageUtil.formatMessage(title)) : null;
        Component subtitleComp = subtitle != null ? Component.text(MessageUtil.formatMessage(subtitle)) : null;

        WrapperPlayServerTitle timePacket = new WrapperPlayServerTitle(
                WrapperPlayServerTitle.TitleAction.SET_TIMES_AND_DISPLAY,
                (Component) null, (Component) null, (Component) null,
                fadeIn, stay, fadeOut
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, timePacket);

        if (titleComp != null) {
            WrapperPlayServerTitle titlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_TITLE,
                    titleComp, null, null,
                    fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, titlePacket);
        }

        if (subtitleComp != null) {
            WrapperPlayServerTitle subtitlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_SUBTITLE,
                    null, subtitleComp, null,
                    fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, subtitlePacket);
        }
    }

    public void kickPlayer(Player player, String reason) {
        WrapperPlayServerDisconnect packet = new WrapperPlayServerDisconnect(Component.text(MessageUtil.formatMessage(reason)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}
