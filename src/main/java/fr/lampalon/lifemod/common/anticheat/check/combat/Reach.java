package fr.lampalon.lifemod.common.anticheat.check.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.UUID;

public class Reach extends AbstractCheck {

    public Reach(IConfigurationService configService) {
        super("Reach", configService);
    }

    @Override
    public PacketTypeCommon[] getSupportedPackets() {
        return new PacketTypeCommon[]{PacketType.Play.Client.INTERACT_ENTITY};
    }

    @Override
    public void onHandle(UUID uuid, ACPlayerData data, Object context) {
        if (!(context instanceof PacketReceiveEvent)) return;
        PacketReceiveEvent event = (PacketReceiveEvent) context;
        
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int targetId = wrapper.getEntityId();
        long ping = data.getPing();

        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
        platform.runTask(() -> {
            Player attacker = Bukkit.getPlayer(uuid);
            if (attacker == null) return;

            if (attacker.getGameMode() == GameMode.CREATIVE) {
                return;
            }

            Entity target = attacker.getWorld().getEntities().stream()
                    .filter(e -> e.getEntityId() == targetId)
                    .findFirst().orElse(null);

            if (target == null) return;

            double distance = getReachDistance(attacker, target);
            double pingBuffer = (ping / 50.0) * 0.11;
            double maxReach = configService.getDouble("anticheat.checks.reach.max_distance", 3.0);
            double threshold = maxReach + pingBuffer + 0.1;

            if (distance > threshold) {
                data.setHitboxBuffer(data.getHitboxBuffer() + 1.0);

                if (data.getHitboxBuffer() > 1.5) {
                    double violationProbability = Math.min(1.0, (distance - threshold) * 2.0);
                    flag(uuid, data, violationProbability,
                            String.format("D: %.2f | Lim: %.2f | P: %dms", distance, threshold, ping));
                }
            } else {
                data.setHitboxBuffer(Math.max(0, data.getHitboxBuffer() - 0.05));
            }
        });
    }

    private double getReachDistance(Player attacker, Entity target) {
        Location eye = attacker.getEyeLocation();
        BoundingBox box = target.getBoundingBox().clone().expand(0.1);

        double closestX = Math.max(box.getMinX(), Math.min(eye.getX(), box.getMaxX()));
        double closestY = Math.max(box.getMinY(), Math.min(eye.getY(), box.getMaxY()));
        double closestZ = Math.max(box.getMinZ(), Math.min(eye.getZ(), box.getMaxZ()));

        Location closestPoint = new Location(target.getWorld(), closestX, closestY, closestZ);
        return eye.distance(closestPoint);
    }
}
