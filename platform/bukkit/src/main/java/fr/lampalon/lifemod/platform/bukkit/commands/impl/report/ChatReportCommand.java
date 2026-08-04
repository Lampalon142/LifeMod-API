package fr.lampalon.lifemod.platform.bukkit.commands.impl.report;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.listeners.ChatReportListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class ChatReportCommand extends LifeCommand {

    public ChatReportCommand() {
        super("reportchat", "lifemod.report", true, "chatreport");
        setDescription("Report a player directly from chat.");
        setUsage("/reportchat <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("chatreport.usage"));
            return;
        }

        Player reporter = context.getPlayer();
        Player target = Bukkit.getPlayer(context.getArgs()[0]);

        if (target == null) {
            context.getSender().sendMessage(context.getLang().getMessage("chatreport.not-found"));
            return;
        }

        if (target.equals(reporter)) {
            context.getSender().sendMessage(context.getLang().getMessage("chatreport.self"));
            return;
        }

        ChatReportListener.beginConfirmation(reporter, target);

        context.getSender().sendMessage(context.getLang().getMessage("chatreport.prompt",
                "%target%", target.getName(),
                "%confirm%", context.getLang().getMessage("chatreport.confirm")));
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}