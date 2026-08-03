package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager;
import org.bukkit.entity.Player;

public class NoClipCommand extends LifeCommand {

    private final LifeMod plugin;

    public NoClipCommand(LifeMod plugin) {
        super("noclip", "lifemod.admin.noclip", true, "nc");
        this.plugin = plugin;
        setDescription("Active ou désactive le mode NoClip (traverse les murs).");
        setUsage("/noclip <on|off>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        NoClipManager noClipManager = plugin.getNoClipManager();
        if (noClipManager == null) {
            player.sendMessage(context.getLang().getMessage("system.module-disabled"));
            return;
        }

        if (context.getArgs().length == 0) {
            noClipManager.toggleNoClip(player);
            return;
        }

        String action = context.getArgs()[0].toLowerCase();
        switch (action) {
            case "on" -> noClipManager.enableNoClip(player);
            case "off" -> noClipManager.disableNoClip(player);
            default -> player.sendMessage(context.getLang().getMessage("commands.noclip.usage"));
        }
    }

    @Override
    public java.util.List<String> onTabComplete(CommandContext context) {
        return java.util.List.of("on", "off");
    }
}
