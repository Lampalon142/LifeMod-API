package fr.lampalon.lifemod.platform.bukkit.nms.v1_21_R1;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTitle;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.common.nms.api.NMSReplayHandler;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Implementation for NMS capabilities using PacketEvents (v1_21_R1 compatible).
 */
public class NMSHandler_v1_21_R1 implements NMSProvider, NMSReplayHandler {

    @Override
    public void spawnNPC(Player spectator, UUID uuid, String name, Location location) {
        int entityId = uuid.hashCode();

        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData playerData =
                new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData(
                        net.kyori.adventure.text.Component.text(name),
                        new com.github.retrooper.packetevents.protocol.player.UserProfile(uuid, name),
                        com.github.retrooper.packetevents.protocol.player.GameMode.SURVIVAL,
                        0
                );

        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo packetInfo = new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo(
                com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
                java.util.Collections.singletonList(playerData)
        );

        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer packetSpawn = new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer(
                entityId,
                uuid,
                new com.github.retrooper.packetevents.protocol.world.Location(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch())
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, packetInfo);
        PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, packetSpawn);
    }

    @Override
    public void removeNPC(Player spectator, UUID uuid) {
        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo packetRemove = new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo(
                com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER,
                java.util.Collections.singletonList(new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo.PlayerData(
                        null,
                        new com.github.retrooper.packetevents.protocol.player.UserProfile(uuid, null),
                        com.github.retrooper.packetevents.protocol.player.GameMode.SURVIVAL,
                        0
                ))
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, packetRemove);
    }

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

    @Override
    public String getName() {
        return "v1_21_R1";
    }
}
