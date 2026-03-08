package fr.lampalon.lifemod.common.anticheat.check.combat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Hitbox extends AbstractCheck {

    public Hitbox(IConfigurationService configService) {
        super("Hitbox", configService);
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{PacketType.Play.Client.INTERACT_ENTITY};
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        if (!(context instanceof WrapperPlayClientInteractEntity)) return;
        WrapperPlayClientInteractEntity wrapper = (WrapperPlayClientInteractEntity) context;

        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
        int targetId = wrapper.getEntityId();

        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);

        long ping = PacketEvents.getAPI().getPlayerManager().getPing(Bukkit.getPlayer(uuid));

        platform.runTask(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) return;

            Entity target = null;
            for (Entity e : player.getNearbyEntities(10, 10, 10)) {
                if (e.getEntityId() == targetId) {
                    target = e;
                    break;
                }
            }

            if (target == null) return;

            Location eye = player.getEyeLocation();
            Vector direction = eye.getDirection();

            double baseMargin = configService.getDouble("anticheat.checks.hitbox.margin", 0.15);

            double pingExpansion = (ping / 50.0) * 0.05;

            BoundingBox targetBox = target.getBoundingBox().clone();

            double expandX = baseMargin + pingExpansion + 0.2;
            double expandY = baseMargin + pingExpansion + 0.4;
            double expandZ = baseMargin + pingExpansion + 0.2;

            targetBox.expand(expandX, expandY, expandZ);

            double maxRange = configService.getDouble("anticheat.checks.hitbox.max_range", 6.0);
            RayTraceResult result = targetBox.rayTrace(eye.toVector(), direction, maxRange);

            double buffer = data.getHitboxBuffer();

            if (result == null) {
                buffer += 1.0;

                if (buffer > 4.0) {
                    flag(uuid, data, 0.9, "Hitbox manquée. Buffer max atteint. (Ping: " + ping + "ms)");
                    buffer = 0;
                }
            } else {
                buffer = Math.max(0, buffer - 0.5);
            }

            data.setHitboxBuffer(buffer);
        });
    }
}