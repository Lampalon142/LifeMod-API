package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StaffchatCommand extends LifeCommand {

    public StaffchatCommand() {
        super("staffchat", "lifemod.staffchat", true);
        setDescription("Send messages to staff members or toggle staff chat mode.");
        setUsage("/staffchat [message]");
    }

    @Override
    public void execute(CommandContext context) {
        if (!context.getConfig().getBoolean("modules.staffchat.enabled", true)) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.staffchat.usage"));
            return;
        }

        Player player = context.getPlayer();
        String[] args = context.getArgs();

        if (args.length == 0) {
            Set<UUID> toggled = ((LifeMod) context.getPlugin()).getStaffChatToggled();
            UUID uuid = player.getUniqueId();
            if (toggled.contains(uuid)) {
                toggled.remove(uuid);
                player.sendMessage(context.getLang().getMessage("commands.staffchat.toggled_off"));
            } else {
                toggled.add(uuid);
                player.sendMessage(context.getLang().getMessage("commands.staffchat.toggled_on"));
            }
            return;
        }

        String message = String.join(" ", args);
        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
        String staffchatMessage = context.getLang().getMessage("commands.staffchat.message",
                "%player%", player.getName(),
                "%message%", message,
                "%server%", platform.getServerName());

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("lifemod.staffchat")) {
                staff.sendMessage(staffchatMessage);
            }
        }
        context.getSender().sendMessage(context.getLang().getMessage("commands.staffchat.success"));
        context.getDebug().log("staffchat", player.getName() + " sent staffchat message: " + message);

        IMessagingService messaging = ServiceRegistry.get(IMessagingService.class);
        if (messaging != null) {
            messaging.publish("lifemod:staff", "CHAT|" + player.getUniqueId() + "|" + player.getName() + "|" + message + "|" + platform.getServerName());
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context, "generic", Map.of("%message%", message));
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return Collections.singletonList("<message>");
        }
        return super.onTabComplete(context);
    }
}
