package fr.lampalon.lifemod.platform.bukkit.nms.v1_21_R1;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.common.nms.api.NMSReplayHandler;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Implementation for NMS capabilities using PacketEvents (v1_21_R1 compatible).
 */
public class NMSHandler_v1_21_R1 implements NMSProvider, NMSReplayHandler {

    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger("NMSHandler_v1_21_R1");

    private final LifeMod plugin;

    public NMSHandler_v1_21_R1(LifeMod plugin) {
        this.plugin = plugin;
    }

    @Override
    public void spawnNPC(Player spectator, int entityId, UUID uuid, String name,
                         TextureProperty[] skin, org.bukkit.Location location) {

        LOGGER.info("[DEBUG] Spawning NPC for " + spectator.getName()
                + ": " + name + " (ID: " + entityId + ")");

        // 1) Build profile with skin
        UserProfile profile = new UserProfile(uuid, name);
        if (skin != null && skin.length > 0) {
            profile.setTextureProperties(Arrays.asList(skin));
        }

        // 2) PlayerInfoUpdate
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

        // 3) SpawnEntity
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

        // 4) Metadata
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

        // 5) Head look + teleport
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

    @Override
    public void sendActionBar(Player player, String message) {
        WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(
                Component.text(MessageUtil.formatMessage(message)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle,
                          int fadeIn, int stay, int fadeOut) {
        Component titleComp    = title    != null ? Component.text(MessageUtil.formatMessage(title))    : null;
        Component subtitleComp = subtitle != null ? Component.text(MessageUtil.formatMessage(subtitle)) : null;

        WrapperPlayServerTitle timePacket = new WrapperPlayServerTitle(
                WrapperPlayServerTitle.TitleAction.SET_TIMES_AND_DISPLAY,
                Component.empty(), Component.empty(), Component.empty(), fadeIn, stay, fadeOut
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, timePacket);

        if (titleComp != null) {
            WrapperPlayServerTitle titlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_TITLE,
                    titleComp, Component.empty(), Component.empty(), fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, titlePacket);
        }

        if (subtitleComp != null) {
            WrapperPlayServerTitle subtitlePacket = new WrapperPlayServerTitle(
                    WrapperPlayServerTitle.TitleAction.SET_SUBTITLE,
                    Component.empty(), subtitleComp, Component.empty(), fadeIn, stay, fadeOut
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, subtitlePacket);
        }
    }

    @Override
    public void kickPlayer(Player player, String reason) {
        WrapperPlayServerDisconnect packet = new WrapperPlayServerDisconnect(
                Component.text(MessageUtil.formatMessage(reason)));
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

    @Override
    public List<Container> getLoadedContainers(World world) {
        List<Container> containers = new ArrayList<>();
        // On utilise l'API Bukkit de base mais de manière optimisée pour éviter la duplication d'objets
        // Si les dépendances NMS ne sont pas présentes au compile-time, on utilise les méthodes natives de Bukkit
        // qui sont déjà très performantes sur les versions récentes de Paper.
        for (Chunk chunk : world.getLoadedChunks()) {
            try {
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Container container) {
                        containers.add(container);
                    }
                }
            } catch (Exception ignored) {}
        }
        return containers;
    }
}
