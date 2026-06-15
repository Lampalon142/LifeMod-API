package fr.lampalon.lifemod.platform.bukkit.managers;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILogService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LogCommandInterceptor implements PacketListener {

    private static final Set<String> IGNORED = new HashSet<>(Arrays.asList(
        "shop", "warp", "spawn", "tpa", "tpahere",
        "bal", "balance", "pay", "msg", "r", "reply",
        "togglechat", "staffchat"
    ));

    private final ILogService logService;
    private final IConfigurationService config;

    public LogCommandInterceptor() {
        this.logService = ServiceRegistry.get(ILogService.class);
        this.config = ServiceRegistry.get(IConfigurationService.class);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (logService == null) return;
        if (!config.getBoolean("logs.log-commands", true)) return;

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {
            String command = event.getPacket().getStrings().read(0, "");
            if (command.isEmpty()) return;

            String base = command.split(" ")[0].toLowerCase();
            if (IGNORED.contains(base)) return;

            User user = event.getUser();
            if (user == null) return;
            UUID uuid = user.getUUID();

            Player player = Bukkit.getPlayer(uuid);
            String name = player != null ? player.getName() : uuid.toString().substring(0, 8);

            logService.log(LogEntry.builder()
                .type(LogType.COMMAND_EXECUTED).playerUuid(uuid).playerName(name)
                .actionData("{\"c\":\"" + jsonEscape("/" + command) + "\"}")
                .serverName(config.getString("server.name", "unknown"))
                .now().build());
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
