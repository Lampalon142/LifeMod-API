package fr.lampalon.lifemod.nms.v1_18_R2;

import fr.lampalon.lifemod.nms.AbstractNmsHandler;
import org.bukkit.plugin.Plugin;

public final class NmsHandlerImpl extends AbstractNmsHandler {

    public NmsHandlerImpl(Plugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "v1_18_R2";
    }
}
