package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeManager {
    private final Map<UUID, ItemStack> playerHelmets = new ConcurrentHashMap<>();
    private final Map<UUID, Location> frozenPlayers = new ConcurrentHashMap<>();
    private final DebugManager debug;

    public FreezeManager() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    public boolean isPlayerFrozen(UUID playerId) {
        return frozenPlayers.containsKey(playerId);
    }

    public void freezePlayer(Player moderator, Player target) {
        debug.log("freeze", "freezePlayer called: moderator=" + moderator.getName() + " target=" + target.getName());
        if (isPlayerFrozen(target.getUniqueId())) {
            debug.log("freeze", "freezePlayer: " + target.getName() + " is already frozen, returning");
            return;
        }

        try {
            ItemStack helmet = target.getInventory().getHelmet();
            debug.log("freeze", "freezePlayer: current helmet=" + (helmet == null ? "null" : helmet.getType().name()));
            if (helmet != null) {
                playerHelmets.put(target.getUniqueId(), helmet.clone());
                debug.log("freeze", "freezePlayer: saved original helmet for " + target.getName());
            }
            frozenPlayers.put(target.getUniqueId(), target.getLocation().clone());
            IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
            String helmetMat = config.getString("modules.mod-mode.items.freeze.helmet-material", "PACKED_ICE");
            debug.log("freeze", "freezePlayer: setting helmet to " + helmetMat);
            target.getInventory().setHelmet(new ItemStack(Material.valueOf(helmetMat)));
            
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            for (String line : lang.getStringList("commands.freeze.messages.freeze.onfreeze")) {
                target.sendMessage(line);
            }
            
            debug.log("freeze", moderator.getName() + " froze " + target.getName() + " | frozenPlayers.size=" + frozenPlayers.size());
        } catch (Exception e) {
            debug.userError(moderator, "Error while freezing " + target.getName(), e);
        }
    }

    public void unfreezePlayer(Player moderator, Player target) {
        debug.log("freeze", "unfreezePlayer called: moderator=" + moderator.getName() + " target=" + target.getName());
        try {
            if (frozenPlayers.containsKey(target.getUniqueId())) {
                target.getInventory().setHelmet(null);
                debug.log("freeze", "unfreezePlayer: removed ice helmet");
                if (playerHelmets.containsKey(target.getUniqueId())) {
                    ItemStack originalHelmet = playerHelmets.get(target.getUniqueId());
                    debug.log("freeze", "unfreezePlayer: restoring original helmet " + originalHelmet.getType());
                    target.getInventory().setHelmet(originalHelmet);
                    playerHelmets.remove(target.getUniqueId());
                }
                frozenPlayers.remove(target.getUniqueId());
                fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                target.sendMessage(lang.getMessage("commands.freeze.messages.unfreeze.target", "%player%", moderator.getName()));
                debug.log("freeze", moderator.getName() + " unfroze " + target.getName() + " | frozenPlayers.size=" + frozenPlayers.size());
            } else {
                debug.log("freeze", "unfreezePlayer: " + target.getName() + " is not in frozenPlayers map!");
            }
        } catch (Exception e) {
            debug.userError(moderator, "Error while unfreezing " + target.getName(), e);
        }
    }

    public void handleQuit(UUID playerId) {
        debug.log("freeze", "handleQuit: " + playerId + " | was in frozenPlayers=" + frozenPlayers.containsKey(playerId));
        playerHelmets.remove(playerId);
        frozenPlayers.remove(playerId);
    }

    public Map<UUID, Location> getFrozenPlayers() {
        return frozenPlayers;
    }
}
