package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.StaffModeManager;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.entity.Player;

public class ModCommand extends LifeCommand {
    private final StaffModeManager staffModeManager;
    private final LifeMod plugin;
    public ModCommand(LifeMod plugin) {
        super("mod", "lifemod.mod", true, "staff");
        this.plugin = plugin;
        this.staffModeManager = plugin.getStaffModeManager();
        setDescription("Toggles staff mode on or off.");
        setUsage("/mod");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (staffModeManager.isMod(player)) {
            staffModeManager.disableStaffMode(player);
        } else {
            staffModeManager.enableStaffMode(player);
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context, "mod");
        }
    }
}
