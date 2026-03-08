package fr.lampalon.lifemod.common.anticheat.check.combat;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import fr.lampalon.lifemod.common.anticheat.check.AbstractCheck;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.UUID;

/**
 * Reach check using Euclidean distance.
 */
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
        if (!(context instanceof WrapperPlayClientInteractEntity)) return;
        WrapperPlayClientInteractEntity wrapper = (WrapperPlayClientInteractEntity) context;
        
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
        int targetId = wrapper.getEntityId();

        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
        platform.runTask(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;

            Entity target = player.getWorld().getEntities().stream()
                    .filter(e -> e.getEntityId() == targetId)
                    .findFirst().orElse(null);

            if (target == null) return;

            double distance = getReachDistance(player, target);
            double maxReach = configService.getDouble("anticheat.checks.reach.max_distance", 3.1);

            if (distance > maxReach) {
                double probability = (distance - maxReach) * 3.0; 
                flag(uuid, data, Math.min(1.0, probability), "Dist: " + String.format("%.3f", distance));
            }
        });
    }

    private double getReachDistance(Player attacker, Entity target) {
        Location eye = attacker.getEyeLocation();
        BoundingBox box = target.getBoundingBox();

        double closestX = Math.max(box.getMinX(), Math.min(eye.getX(), box.getMaxX()));
        double closestY = Math.max(box.getMinY(), Math.min(eye.getY(), box.getMaxY()));
        double closestZ = Math.max(box.getMinZ(), Math.min(eye.getZ(), box.getMaxZ()));

        return eye.distance(new Location(target.getWorld(), closestX, closestY, closestZ));
    }
}
