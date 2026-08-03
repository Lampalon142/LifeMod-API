package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.platform.bukkit.replay.ReplayCodec;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class ReplayEventRecorder implements Listener {

    private static final Logger LOGGER = Logger.getLogger("ReplayEventRecorder");
    private final ReplayManager replayManager;

    public ReplayEventRecorder(ReplayManager replayManager) {
        if (replayManager == null) throw new IllegalArgumentException("replayManager cannot be null");
        this.replayManager = replayManager;
    }

    /**
     * Builds a single-magic-byte event frame and queues it on the recording session.
     * Shared by every event handler to avoid repeating the stream/queue boilerplate.
     */
    @FunctionalInterface
    private interface DataWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private void recordEvent(ReplaySession session, byte magic, DataWriter body) {
        if (session == null || !session.isRecording()) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(magic);
            body.write(dos);
            session.queuePacket(baos.toByteArray());
        } catch (Exception e) {
            LOGGER.warning("ReplayEventRecorder: event " + magic + " failed " + e.getMessage());
        }
    }

    private void recordEvent(UUID playerUuid, byte magic, DataWriter body) {
        recordEvent(replayManager.getSession(playerUuid), magic, body);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        var stack = event.getItemDrop().getItemStack();
        recordEvent(event.getPlayer().getUniqueId(), ReplayCodec.EVENT_DROP, dos -> {
            dos.writeUTF(stack.getType().name());
            dos.writeInt(stack.getAmount());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        recordEvent(event.getPlayer().getUniqueId(), ReplayCodec.EVENT_CHAT, dos -> dos.writeUTF(event.getMessage()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        recordEvent(event.getPlayer().getUniqueId(), ReplayCodec.EVENT_COMMAND, dos -> dos.writeUTF(event.getMessage()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity attacked = event.getEntity();
        Entity damager = event.getDamager();

        ReplaySession victimSession = attacked instanceof Player p
                ? replayManager.getSession(p.getUniqueId()) : null;
        ReplaySession attackerSession = damager instanceof Player p
                ? replayManager.getSession(p.getUniqueId()) : null;

        if (attacked instanceof Player victim) {
            recordEvent(victimSession, ReplayCodec.EVENT_DAMAGE, dos -> {
                dos.writeUTF(damager.getName());
                dos.writeDouble(event.getFinalDamage());
                dos.writeUTF(event.getCause().name());
            });
            Entity mob = resolveAttackerMob(damager);
            if (mob != null) spawnCombatMob(victimSession, mob);
        }
        if (damager instanceof Player attacker && !(attacked instanceof Player)) {
            recordEvent(attackerSession, ReplayCodec.EVENT_DAMAGE, dos -> {
                dos.writeUTF(attacked.getName());
                dos.writeDouble(event.getFinalDamage());
                dos.writeUTF(event.getCause().name());
            });
            if (attacked instanceof LivingEntity && !(attacked instanceof Player)) {
                spawnCombatMob(attackerSession, attacked);
            }
        }
        if (damager instanceof Player attacker && attacked instanceof Player victim
                && !attacker.equals(victim)) {
            recordEvent(attackerSession, ReplayCodec.EVENT_DAMAGE, dos -> {
                dos.writeUTF(victim.getName());
                dos.writeDouble(event.getFinalDamage());
                dos.writeUTF(event.getCause().name());
            });
        }
    }

    private Entity resolveAttackerMob(Entity damager) {
        if (damager instanceof LivingEntity && !(damager instanceof Player)) return damager;
        if (damager instanceof Projectile p && p.getShooter() instanceof LivingEntity s
                && !(s instanceof Player)) return s;
        return null;
    }

    private void spawnCombatMob(ReplaySession session, Entity mob) {
        if (session == null || !session.isRecording()) return;
        if (!(mob instanceof LivingEntity) || mob instanceof Player) return;
        int eId = mob.getEntityId();
        if (session.isEntityActive(eId)) return;
        byte[] frame = ReplayCodec.buildSpawnMobFrame(eId, mob.getType().name(),
                mob.getLocation().getX(), mob.getLocation().getY(), mob.getLocation().getZ(),
                mob.getLocation().getYaw(), mob.getLocation().getPitch());
        if (frame == null) return;
        session.trackSpawn(eId);
        session.queuePacket(frame);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        recordEvent(event.getEntity().getUniqueId(), ReplayCodec.EVENT_DEATH,
                dos -> dos.writeUTF(event.getDeathMessage() != null ? event.getDeathMessage() : ""));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        recordEvent(shooter.getUniqueId(), ReplayCodec.EVENT_PROJECTILE, dos -> dos.writeUTF(event.getEntity().getType().name()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        recordEvent(event.getPlayer().getUniqueId(), ReplayCodec.EVENT_INTERACT, dos -> {
            dos.writeUTF(event.getRightClicked().getName());
            dos.writeUTF(event.getRightClicked().getType().name());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var stack = event.getItem().getItemStack();
        recordEvent(player.getUniqueId(), ReplayCodec.EVENT_PICKUP, dos -> {
            dos.writeUTF(stack.getType().name());
            dos.writeInt(stack.getAmount());
        });
    }
}
