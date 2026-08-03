package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NoClipCommand extends LifeCommand {

    private final LifeMod plugin;

    public NoClipCommand(LifeMod plugin) {
        super("noclip", "lifemod.admin.noclip", true, "nc");
        this.plugin = plugin;
        setDescription("Active ou désactive le NoClip (traverse les murs en vol créatif).");
        setUsage("/noclip [on|off]");
    }

    @Override
    public void execute(CommandContext context) {
        NoClipManager noClipManager = plugin.getNoClipManager();
        if (noClipManager == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.module-disabled"));
            return;
        }

        if (context.getArgs().length == 0) {
            noClipManager.toggleNoClip(context.getPlayer());
            return;
        }

        switch (context.getArgs()[0].toLowerCase()) {
            case "on" -> noClipManager.enableNoClip(context.getPlayer());
            case "off" -> noClipManager.disableNoClip(context.getPlayer());
            default -> context.getSender().sendMessage(context.getLang().getMessage("commands.noclip.usage"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return Arrays.asList("on", "off").stream()
                    .filter(s -> s.startsWith(context.getArgs()[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.onTabComplete(context);
    }
}