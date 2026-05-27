package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FreezeManager {
    private final Map<UUID, ItemStack> playerHelmets = new HashMap<>();
    private final Map<UUID, Location> frozenPlayers = new HashMap<>();
    private final DebugManager debug;

    public FreezeManager() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    public boolean isPlayerFrozen(UUID playerId) {
        return frozenPlayers.containsKey(playerId);
    }

    public void freezePlayer(Player moderator, Player target) {
        if (isPlayerFrozen(target.getUniqueId())) return;

        try {
            ItemStack helmet = target.getInventory().getHelmet();
            if (helmet != null) {
                playerHelmets.put(target.getUniqueId(), helmet);
            }
            frozenPlayers.put(target.getUniqueId(), target.getLocation());
            target.getInventory().setHelmet(new ItemStack(Material.PACKED_ICE));
            
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            for (String line : lang.getStringList("commands.freeze.messages.freeze.onfreeze")) {
                target.sendMessage(line);
            }
            
            debug.log("freeze", moderator.getName() + " froze " + target.getName());
        } catch (Exception e) {
            debug.userError(moderator, "Error while freezing " + target.getName(), e);
        }
    }

    public void unfreezePlayer(Player moderator, Player target) {
        try {
            if (frozenPlayers.containsKey(target.getUniqueId())) {
                target.getInventory().setHelmet(null);
                if (playerHelmets.containsKey(target.getUniqueId())) {
                    target.getInventory().setHelmet(playerHelmets.get(target.getUniqueId()));
                    playerHelmets.remove(target.getUniqueId());
                }
                frozenPlayers.remove(target.getUniqueId());
                fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                target.sendMessage(lang.getMessage("commands.freeze.messages.unfreeze.target", "%player%", moderator.getName()));
                debug.log("freeze", moderator.getName() + " unfroze " + target.getName());
            }
        } catch (Exception e) {
            debug.userError(moderator, "Error while unfreezing " + target.getName(), e);
        }
    }

    public void handleQuit(UUID playerId) {
        playerHelmets.remove(playerId);
        frozenPlayers.remove(playerId);
    }

    public Map<UUID, Location> getFrozenPlayers() {
        return frozenPlayers;
    }
}
