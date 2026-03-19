package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.replay.gui.ReplayInventoryGui;
import org.bukkit.entity.Player;

/**
 * Command for initiating a replay playback.
 */
public class ReplayCommand extends LifeCommand {

    public ReplayCommand(LifeMod plugin) {
        super("replay", "replay.use", false, new String[0]);
    }

    @Override
    public void execute(CommandContext context) {
        if (!(context.getSender() instanceof Player)) {
            context.getSender().sendMessage("§cOnly players can use this command.");
            return;
        }

        Player mod = (Player) context.getSender();
        
        if (context.getArgs().length < 1) {
            mod.sendMessage("§cUsage: /replay <player>");
            return;
        }

        String targetName = context.getArgs()[0];
        
        // In a real implementation, we would search for available .replay files for this player
        mod.sendMessage("§aOpening replay session for §e" + targetName + "§a...");
        
        mod.setGameMode(org.bukkit.GameMode.SPECTATOR);
        new ReplayInventoryGui(mod).open();
    }
}
