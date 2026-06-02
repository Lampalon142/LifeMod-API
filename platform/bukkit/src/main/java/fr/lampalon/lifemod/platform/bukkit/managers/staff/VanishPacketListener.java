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

        // Block Player Info Update (TabList & Spawn)
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
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
        
        // Legacy support (older versions)
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
            WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo(event);
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
