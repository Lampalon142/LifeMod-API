package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import org.bukkit.entity.Player;

public class NoClipCommand extends LifeCommand {

    private final LifeMod plugin;

    public NoClipCommand(LifeMod plugin) {
        super("noclip", "lifemod.admin.noclip", true, "nc");
        this.plugin = plugin;
        setDescription("Active ou désactive le mode NoClip (traverse les murs).");
        setUsage("/noclip");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        plugin.getNoClipManager().toggleNoClip(player);
    }
}