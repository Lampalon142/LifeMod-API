package fr.lampalon.lifemod.platform.bukkit.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CPSListener implements Listener, PacketListener {
    private final Map<UUID, Deque<Long>> cpsMap;

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
    }
}

