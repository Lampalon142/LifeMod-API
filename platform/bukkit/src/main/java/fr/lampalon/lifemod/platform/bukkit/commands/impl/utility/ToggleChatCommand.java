package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;


import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;



public class ToggleChatCommand extends LifeCommand {

    public ToggleChatCommand() {
        super("togglechat", "lifemod.togglechat", false);
        setDescription("Toggles the global chat on or off.");
        setUsage("/togglechat");
    }

    @Override
    public void execute(CommandContext context) {
        boolean newState = !context.getPlugin().isChatEnabled();
        context.getPlugin().setChatEnabled(newState);

        String msgKey = newState ? "togglechat.enabled" : "togglechat.disabled";
        context.getSender().sendMessage(context.getLang().getMessage(msgKey));

    }
}
