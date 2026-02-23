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
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Hitbox check using server-side Ray-tracing.
 */
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
        if (!(context instanceof PacketReceiveEvent)) return;
        PacketReceiveEvent event = (PacketReceiveEvent) context;

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
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

                Location eye = player.getEyeLocation();
                Vector direction = eye.getDirection();
                
                double maxRange = configService.getDouble("anticheat.checks.hitbox.max_range", 6.0);
                BoundingBox targetBox = target.getBoundingBox();
                
                double margin = configService.getDouble("anticheat.checks.hitbox.margin", 0.1);
                targetBox.expand(margin);

                RayTraceResult result = targetBox.rayTrace(eye.toVector(), direction, maxRange);

                if (result == null) {
                    flag(uuid, data, 0.9, "No intersection with BoundingBox");
                }
            });
        }
    }
}
