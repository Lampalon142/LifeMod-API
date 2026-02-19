package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;

public class LifemodCommand extends LifeCommand {

    public LifemodCommand() {
        super("lifemod", "lifemod.admin", false);
        setDescription("Main command for LifeMod");
        setUsage("/lifemod <reload|info>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length == 0) {
            sendHelp(context);
            return;
        }

        if (context.getArgs()[0].equalsIgnoreCase("reload")) {
            if (!context.getSender().hasPermission("lifemod.admin")) {
                context.getSender().sendMessage(context.getLang().getMessage("system.no-permission"));
                return;
            }
            context.getPlugin().reloadPluginConfig();
            context.getPlugin().reloadLangConfig();
            context.getSender().sendMessage(context.getLang().getMessage("commands.lifemod.reload"));
            return;
        }

        if (context.getArgs()[0].equalsIgnoreCase("info")) {
            for (String line : context.getPlugin().getConfigConfig().getStringList("lifemod.info")) {
                context.getSender().sendMessage(line);
            }
            return;
        }
        
        sendHelp(context);
    }

    private void sendHelp(CommandContext context) {
        context.getSender().sendMessage(context.getLang().getMessage("commands.lifemod.help.header"));
        context.getSender().sendMessage(context.getLang().getMessage("commands.lifemod.help.reload"));
        context.getSender().sendMessage(context.getLang().getMessage("commands.lifemod.help.info"));
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filter(Arrays.asList("reload", "info"), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
