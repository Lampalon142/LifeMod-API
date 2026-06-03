package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;

import java.util.HashMap;
import java.util.Map;

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

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("action", newState ? "unlock" : "lock");
            ph.capture("lifemod_chat_toggle", props);
        }
    }
}
