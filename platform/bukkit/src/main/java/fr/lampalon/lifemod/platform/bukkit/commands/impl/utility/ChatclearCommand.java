package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatclearCommand extends LifeCommand {

    public ChatclearCommand() {
        super("chatclear", "lifemod.chatclear", false);
        setDescription("Clears the chat for all players.");
        setUsage("/chatclear");
    }

    @Override
    public void execute(CommandContext context) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                player.sendMessage("");
            }
            player.sendMessage(context.getLang().getMessage("commands.chatclear.message"));
        }
        context.getDebug().log("chatclear", "Chat cleared by " + context.getSender().getName());

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context);
        }
    }
}
