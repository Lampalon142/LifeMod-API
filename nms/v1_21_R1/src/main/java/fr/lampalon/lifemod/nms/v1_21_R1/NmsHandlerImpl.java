package fr.lampalon.lifemod.nms.v1_21_R1;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import fr.lampalon.lifemod.nms.AbstractNmsHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class NmsHandlerImpl extends AbstractNmsHandler {

    public NmsHandlerImpl(Plugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "v1_21_R1";
    }

    @Override
    public void spawnNPC(Player spectator, int entityId, UUID uuid, String name,
                         TextureProperty[] skin, Location location) {
        UserProfile profile = new UserProfile(uuid, name);
        if (skin != null && skin.length > 0) {
            profile.setTextureProperties(Arrays.asList(skin));
        }

        WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo =
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(profile);
        playerInfo.setGameMode(GameMode.SURVIVAL);
        playerInfo.setDisplayName(Component.text(name));
        playerInfo.setListed(false);

        WrapperPlayServerPlayerInfoUpdate packetAddTab = new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(
                        WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME
                ),
                Collections.singletonList(playerInfo)
        );

        WrapperPlayServerSpawnEntity packetSpawn = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.PLAYER,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(),
                location.getYaw(),
                location.getYaw(),
                0,
                Optional.empty()
        );

        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(
                entityId,
                Collections.singletonList(
                        new com.github.retrooper.packetevents.protocol.entity.data.EntityData(
                                17,
                                com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE,
                                (byte) 127
                        )
                )
        );

        WrapperPlayServerEntityHeadLook headLook =
                new WrapperPlayServerEntityHeadLook(entityId, location.getYaw());
        WrapperPlayServerEntityTeleport teleport =
                new WrapperPlayServerEntityTeleport(
                        entityId,
                        new Vector3d(location.getX(), location.getY(), location.getZ()),
                        location.getYaw(), location.getPitch(), true
                );

        PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, packetAddTab);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!spectator.isOnline()) return;

            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, packetSpawn);
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, metadata);
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, headLook);
            PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, teleport);
        }, 3L);
    }

    @Override
    public void removeNPC(Player spectator, int entityId, UUID uuid) {
        WrapperPlayServerPlayerInfoRemove packetRemoveTab =
                new WrapperPlayServerPlayerInfoRemove(Collections.singletonList(uuid));
        WrapperPlayServerDestroyEntities packetDestroy =
                new WrapperPlayServerDestroyEntities(entityId);

        PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, packetRemoveTab);
        PacketEvents.getAPI().getPlayerManager().sendPacket(spectator, packetDestroy);
    }
}
