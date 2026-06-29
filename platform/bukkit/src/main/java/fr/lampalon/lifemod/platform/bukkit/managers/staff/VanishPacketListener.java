package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VanishPacketListener implements PacketListener {

    private final IVanishService vanishService;

    public VanishPacketListener(IVanishService vanishService) {
        this.vanishService = vanishService;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player receiver = (Player) event.getPlayer();
        if (receiver == null || receiver.hasPermission("lifemod.vanish.see")) return;

        // Always let PLAYER_INFO_REMOVE through — this is how the vanished player
        // gets removed from the tab list (modern clients).
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_REMOVE) {
            return;
        }

        // Only filter ADD_PLAYER actions to prevent re-adding vanished players.
        // Let other actions (UPDATE_LISTED, UPDATE_LATENCY, etc.) through so
        // hidePlayer() can actually hide the player from the tab list.
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);

            if (!wrapper.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER)) {
                return;
            }

            List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> entries = new ArrayList<>(wrapper.getEntries());
            boolean modified = entries.removeIf(entry -> vanishService.isVanished(entry.getProfileId()));

            if (modified) {
                if (entries.isEmpty()) {
                    event.setCancelled(true);
                } else {
                    wrapper.setEntries(entries);
                }
            }
        }

        // Legacy support: only filter ADD_PLAYER, let REMOVE_PLAYER through
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
            WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo(event);

            if (wrapper.getAction() != WrapperPlayServerPlayerInfo.Action.ADD_PLAYER) {
                return;
            }

            List<WrapperPlayServerPlayerInfo.PlayerData> entries = new ArrayList<>(wrapper.getPlayerDataList());
            boolean modified = entries.removeIf(entry -> vanishService.isVanished(entry.getUser().getUUID()));

            if (modified) {
                if (entries.isEmpty()) {
                    event.setCancelled(true);
                } else {
                    wrapper.setPlayerDataList(entries);
                }
            }
        }
    }
}
