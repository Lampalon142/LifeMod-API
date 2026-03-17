package fr.lampalon.lifemod.platform.bukkit.nms.v1_21_R1;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTitle;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Implementation for NMS capabilities using PacketEvents (v1_21_R1 compatible).
 */
public class NMSHandler_v1_21_R1 implements NMSProvider {

    @Override
    public void sendActionBar(Player player, String message) {
        WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(Component.text(MessageUtil.formatMessage(message)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
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

    @Override
    public void kickPlayer(Player player, String reason) {
        WrapperPlayServerDisconnect packet = new WrapperPlayServerDisconnect(Component.text(MessageUtil.formatMessage(reason)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
    public int getPing(Player player) {
        return player.getPing();
    }
}
