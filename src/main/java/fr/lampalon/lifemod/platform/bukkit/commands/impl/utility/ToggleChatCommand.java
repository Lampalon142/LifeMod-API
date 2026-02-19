package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;

public class ToggleChatCommand extends LifeCommand {
    private final LifeMod plugin;

    public ToggleChatCommand(LifeMod plugin) {
        super("togglechat", "lifemod.togglechat", false);
        this.plugin = plugin;
        setDescription("Toggles the global chat on or off.");
        setUsage("/togglechat");
    }

    @Override
    public void execute(CommandContext context) {
        boolean newState = !plugin.isChatEnabled();
        plugin.setChatEnabled(newState);

        String msgKey = newState ? "togglechat.enabled" : "togglechat.disabled";
        context.getSender().sendMessage(context.getLang().getMessage(msgKey));
    }
}
