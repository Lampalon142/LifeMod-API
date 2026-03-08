package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.common.anticheat.AntiCheatService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;

import java.util.Arrays;
import java.util.List;

public class AntiCheatCommand extends LifeCommand {

    public AntiCheatCommand() {
        super("anticheat", "lifemod.admin.anticheat", false);
        setDescription("Manage the AntiCheat system.");
        setUsage("/anticheat <reload|status>");
    }

    @Override
    public void execute(CommandContext context) {
        AntiCheatService acService = ServiceRegistry.get(AntiCheatService.class);
        if (acService == null) {
            context.getSender().sendMessage("§cAntiCheat service is not available.");
            return;
        }

        if (context.getArgs().length == 0) {
            context.getSender().sendMessage("§6§lLifeMod AntiCheat §7- §fStatus: " + (acService != null ? "§aActive" : "§cDisabled"));
            context.getSender().sendMessage("§eUsage: /anticheat <reload|status>");
            return;
        }

        String sub = context.getArgs()[0].toLowerCase();
        if (sub.equals("reload")) {
            acService.terminate();
            acService.init();
            context.getSender().sendMessage("§aAntiCheat system reloaded successfully.");
        } else if (sub.equals("status")) {
            context.getSender().sendMessage("§6§lAntiCheat Status:");
            context.getSender().sendMessage("§e• Checks: §fActive");
            context.getSender().sendMessage("§e• Platform: §f" + context.getPlugin().getServer().getName());
        } else {
            context.getSender().sendMessage("§cUnknown subcommand. Use /anticheat <reload|status>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filter(Arrays.asList("reload", "status"), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
