package fr.lampalon.lifemod.platform.bukkit.replay.listeners;

import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import org.bukkit.entity.Player;
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
import java.util.Collections;
import java.util.logging.Logger;

public class ReplayEventRecorder implements Listener {

    private static final Logger LOGGER = Logger.getLogger("ReplayEventRecorder");
    private final ReplayManager replayManager;

    public ReplayEventRecorder(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0xF1);
            dos.writeUTF(event.getItemDrop().getItemStack().getType().name());
            dos.writeInt(event.getItemDrop().getItemStack().getAmount());
            session.addFrame(new ReplayFrame(System.currentTimeMillis(),
                    Collections.singletonList(baos.toByteArray())));
        } catch (Exception e) {
            LOGGER.warning("ReplayEventRecorder: drop failed " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0xF2);
            dos.writeUTF(event.getMessage());
            session.addFrame(new ReplayFrame(System.currentTimeMillis(),
                    Collections.singletonList(baos.toByteArray())));
        } catch (Exception e) {
            LOGGER.warning("ReplayEventRecorder: chat failed " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0xF3);
            dos.writeUTF(event.getMessage());
            session.addFrame(new ReplayFrame(System.currentTimeMillis(),
                    Collections.singletonList(baos.toByteArray())));
        } catch (Exception e) {
            LOGGER.warning("ReplayEventRecorder: command failed " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        ReplaySession session = replayManager.getSession(victim.getUniqueId());
        if (session == null || !session.isRecording()) return;
        try {
            String attacker = event.getDamager().getName();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0xF4);
            dos.writeUTF(attacker);
            dos.writeDouble(event.getFinalDamage());
            dos.writeUTF(event.getCause().name());
            session.addFrame(new ReplayFrame(System.currentTimeMillis(),
                    Collections.singletonList(baos.toByteArray())));
        } catch (Exception e) {
            LOGGER.warning("ReplayEventRecorder: damage failed " + e.getMessage());
        }
        // Also record for the attacker if they are being recorded
        if (event.getDamager() instanceof Player attacker) {
            ReplaySession atkSession = replayManager.getSession(attacker.getUniqueId());
            if (atkSession != null && atkSession.isRecording() && !attacker.equals(victim)) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeByte(0xF4);
                    dos.writeUTF(victim.getName());
                    dos.writeDouble(event.getFinalDamage());
                    dos.writeUTF(event.getCause().name());
                    atkSession.addFrame(new ReplayFrame(System.currentTimeMillis(),
                            Collections.singletonList(baos.toByteArray())));
                } catch (Exception e) {
                    LOGGER.warning("ReplayEventRecorder: damage-attacker failed " + e.getMessage());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        ReplaySession session = replayManager.getSession(event.getEntity().getUniqueId());
        if (session == null || !session.isRecording()) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0xF5);
            dos.writeUTF(event.getDeathMessage() != null ? event.getDeathMessage() : "");
            session.addFrame(new ReplayFrame(System.currentTimeMillis(),
                    Collections.singletonList(baos.toByteArray())));
        } catch (Exception e) {
            LOGGER.warning("ReplayEventRecorder: death failed " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        ReplaySession session = replayManager.getSession(shooter.getUniqueId());
        if (session == null || !session.isRecording()) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0xF6);
            dos.writeUTF(event.getEntity().getType().name());
            session.addFrame(new ReplayFrame(System.currentTimeMillis(),
                    Collections.singletonList(baos.toByteArray())));
        } catch (Exception e) {
            LOGGER.warning("ReplayEventRecorder: projectile failed " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        ReplaySession session = replayManager.getSession(event.getPlayer().getUniqueId());
        if (session == null || !session.isRecording()) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0xF7);
            dos.writeUTF(event.getRightClicked().getName());
            dos.writeUTF(event.getRightClicked().getType().name());
            session.addFrame(new ReplayFrame(System.currentTimeMillis(),
                    Collections.singletonList(baos.toByteArray())));
        } catch (Exception e) {
            LOGGER.warning("ReplayEventRecorder: interact failed " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ReplaySession session = replayManager.getSession(player.getUniqueId());
        if (session == null || !session.isRecording()) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(0xF8);
            dos.writeUTF(event.getItem().getItemStack().getType().name());
            dos.writeInt(event.getItem().getItemStack().getAmount());
            session.addFrame(new ReplayFrame(System.currentTimeMillis(),
                    Collections.singletonList(baos.toByteArray())));
        } catch (Exception e) {
            LOGGER.warning("ReplayEventRecorder: pickup failed " + e.getMessage());
        }
    }
}
