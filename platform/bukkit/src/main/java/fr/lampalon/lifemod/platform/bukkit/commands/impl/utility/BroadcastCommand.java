package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BroadcastCommand extends LifeCommand {
    public BroadcastCommand() {
        super("broadcast", "lifemod.bc", false, "bc");
        setDescription("Broadcasts a message to the entire server.");
        setUsage("/broadcast <message>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.broadcast.usage"));
            return;
        }

        String message = String.join(" ", context.getArgs());
        String formatted = context.getLang().getMessage("commands.broadcast.format", "%message%", message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage(formatted);

        context.getDebug().log("broadcast", "Broadcast sent by " + context.getSender().getName() + ": " + message);

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context, "generic", Map.of("%message%", message));
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            List<String> suggestions = context.getLang().getStringList("commands.broadcast.tab-completer");
            return TabCompleterUtils.filter(suggestions, context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
