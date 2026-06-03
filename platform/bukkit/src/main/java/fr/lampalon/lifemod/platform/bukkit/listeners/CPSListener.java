package fr.lampalon.lifemod.platform.bukkit.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CPSListener implements Listener, PacketListener {
    private final Map<UUID, Deque<Long>> cpsMap;
    private static final int CPS_HIGH_THRESHOLD = 15;

    public CPSListener(Map<UUID, Deque<Long>> cpsMap) {
        this.cpsMap = cpsMap;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            Player player = (Player) event.getPlayer();
            if (player != null) {
                addClick(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // We use packets for better accuracy, but keep this as fallback or for interact-specific logic
        // addClick(event.getPlayer()); 
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        // if (event.getDamager() instanceof Player) {
        //     addClick((Player) event.getDamager());
        // }
    }

    public void cleanup(UUID uuid) {
        cpsMap.remove(uuid);
    }

    private void addClick(Player player) {
        UUID uuid = player.getUniqueId();
        Deque<Long> deque = cpsMap.computeIfAbsent(uuid, k -> new ConcurrentLinkedDeque<>());
        long now = System.currentTimeMillis();
        
        // Prevent double counting if both packet and event fire (though we disabled event counting above)
        if (!deque.isEmpty() && now - deque.peekLast() < 5) {
            return;
        }

        deque.addLast(now);
        
        // Clean up clicks older than 20 seconds to keep memory low
        while (!deque.isEmpty() && now - deque.peekFirst() > 20000) {
            deque.removeFirst();
        }

        int cps = computeCPS(deque, now);
        if (cps >= CPS_HIGH_THRESHOLD) {
            IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
            if (ph != null) {
                Map<String, Object> props = new HashMap<>();
                props.put("cps_value", cps);
                ph.capture("lifemod_cps_high", props);
            }
        }
    }

    public static int computeCPS(Deque<Long> deque, long now) {
        if (deque == null || deque.isEmpty()) return 0;
        long cutoff = now - 1000;
        int count = 0;
        for (long click : deque) {
            if (click >= cutoff) count++;
        }
        return count;
    }
}

