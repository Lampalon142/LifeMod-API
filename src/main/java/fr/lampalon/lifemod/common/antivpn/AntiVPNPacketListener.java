package fr.lampalon.lifemod.common.antivpn;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;

import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import net.kyori.adventure.text.Component;
import fr.lampalon.lifemod.common.utils.ColorUtil;
import java.net.InetSocketAddress;

public class AntiVPNPacketListener implements PacketListener {

    private final AntiVPNService antiVPNService;
    private final ILifePlatform platform;

    public AntiVPNPacketListener(AntiVPNService antiVPNService, ILifePlatform platform) {
        this.antiVPNService = antiVPNService;
        this.platform = platform;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
            WrapperLoginClientLoginStart loginStart = new WrapperLoginClientLoginStart(event);
            String name = loginStart.getUsername();
            
            if (event.getUser() == null || event.getUser().getAddress() == null) return;
            InetSocketAddress address = (InetSocketAddress) event.getUser().getAddress();
            String ip = address.getAddress().getHostAddress();

            antiVPNService.shouldAllowConnection(ip, name).thenAccept(allowed -> {
                if (!allowed) {
                    ILangService lang = ServiceRegistry.get(ILangService.class);
                    String reason = lang != null ? lang.getMessage("antivpn.kick-reason", "vpn.anti-vpn.default") : "vpn.anti-vpn.default";
                    
                    WrapperLoginServerDisconnect disconnect = new WrapperLoginServerDisconnect(Component.text(ColorUtil.format(reason)));
                    event.getUser().sendPacket(disconnect);
                    event.getUser().closeConnection();
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        }
    }
}
