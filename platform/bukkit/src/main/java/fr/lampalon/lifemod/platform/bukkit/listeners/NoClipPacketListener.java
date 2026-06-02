package fr.lampalon.lifemod.platform.bukkit.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class NoClipPacketListener extends PacketListenerAbstract {

    private final NoClipManager noClipManager;

    public NoClipPacketListener(NoClipManager noClipManager) {
        super(PacketListenerPriority.HIGHEST);
        this.noClipManager = noClipManager;
    }

    // =========================================================================
    // CLIENT → SERVEUR
    // =========================================================================

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!noClipManager.isNoClip(player.getUniqueId())) return;

        // --- Mouvement : force on_ground = false ---
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {

            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
            if (wrapper.isOnGround()) wrapper.setOnGround(false);
            return;
        }

        // --- Dig : casse les blocs manuellement car SPECTATOR ne peut pas ---
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
            DiggingAction action = wrapper.getAction();

            // On traite uniquement FINISHED_DIGGING (survie) et START_DIGGING (créatif instamine)
            if (action != DiggingAction.FINISHED_DIGGING && action != DiggingAction.START_DIGGING) return;

            Vector3i pos = wrapper.getBlockPosition();
            GameMode originalGm = noClipManager.getOriginalGameMode(player.getUniqueId());

            // En mode créatif original → instamine sur START_DIGGING
            // En mode survie original → casse sur FINISHED_DIGGING
            boolean shouldBreak = (originalGm == GameMode.CREATIVE && action == DiggingAction.START_DIGGING)
                    || (originalGm != GameMode.CREATIVE && action == DiggingAction.FINISHED_DIGGING);

            if (!shouldBreak) return;

            // Annule le packet original (le serveur SPECTATOR l'ignorerait de toute façon)
            event.setCancelled(true);

            // Exécute la casse sur le thread principal Bukkit
            Bukkit.getScheduler().runTask(LifeMod.getInstance(), () -> {
                Block block = player.getWorld().getBlockAt(pos.x, pos.y, pos.z);

                if (block.isEmpty()) return;

                // Vérifie la distance (anti-abuse, max 6 blocs)
                Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);
                if (player.getLocation().distanceSquared(blockLoc) > 36) return;

                if (originalGm == GameMode.CREATIVE) {
                    // Créatif : casse sans drop
                    block.setType(org.bukkit.Material.AIR);
                } else {
                    // Survie : casse avec drops naturels et prise en compte de l'outil
                    block.breakNaturally(player.getInventory().getItemInMainHand());
                }
            });
        }
    }

    // =========================================================================
    // SERVEUR → CLIENT : spoof du GameMode affiché
    // =========================================================================

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!noClipManager.isNoClip(player.getUniqueId())) return;

        if (event.getPacketType() != PacketType.Play.Server.CHANGE_GAME_STATE) return;

        WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);

        if (packet.getReason() != WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) return;

        float sentGameMode = packet.getValue();

        if (sentGameMode == 3.0f) {
            event.setCancelled(true);

            float originalValue = switch (noClipManager.getOriginalGameMode(player.getUniqueId())) {
                case SURVIVAL  -> 0.0f;
                case CREATIVE  -> 1.0f;
                case ADVENTURE -> 2.0f;
                default        -> 0.0f;
            };

            WrapperPlayServerChangeGameState spoofed = new WrapperPlayServerChangeGameState(
                    WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE,
                    originalValue
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spoofed);
        }
    }
}