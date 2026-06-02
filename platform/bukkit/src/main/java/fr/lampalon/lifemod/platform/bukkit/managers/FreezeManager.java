package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;
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
    private int taskId = -1;

    public FreezeManager() {
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    public void startEnforcement() {
        if (taskId != -1) return;
        taskId = Bukkit.getScheduler().runTaskTimer(LifeMod.getInstance(), () -> {
            for (Map.Entry<UUID, Location> entry : frozenPlayers.entrySet()) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p == null || !p.isOnline()) continue;
                Location target = entry.getValue();
                if (target == null) continue;
                if (p.getLocation().distanceSquared(target) > 0.1) {
                    p.teleport(target);
                }
            }
        }, 1L, 1L).getTaskId();
    }

    private void stopEnforcement() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
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
            if (helmet != null) {
                playerHelmets.put(target.getUniqueId(), helmet.clone());
            }
            frozenPlayers.put(target.getUniqueId(), target.getLocation().clone());
            IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
            String helmetMat = config.getString("modules.mod-mode.items.freeze.helmet-material", "PACKED_ICE");
            target.getInventory().setHelmet(new ItemStack(Material.valueOf(helmetMat)));

            ILangService lang = ServiceRegistry.get(ILangService.class);
            for (String line : lang.getStringList("commands.freeze.messages.freeze.onfreeze")) {
                target.sendMessage(line);
            }

            int count = frozenPlayers.size();
            if (count == 1) startEnforcement();

            debug.log("freeze", moderator.getName() + " froze " + target.getName() + " | frozenPlayers.size=" + count);
        } catch (Exception e) {
            debug.userError(moderator, "Error while freezing " + target.getName(), e);
        }
    }

    public void unfreezePlayer(Player moderator, Player target) {
        debug.log("freeze", "unfreezePlayer called: moderator=" + moderator.getName() + " target=" + target.getName());
        try {
            if (frozenPlayers.containsKey(target.getUniqueId())) {
                target.getInventory().setHelmet(null);
                if (playerHelmets.containsKey(target.getUniqueId())) {
                    ItemStack originalHelmet = playerHelmets.get(target.getUniqueId());
                    target.getInventory().setHelmet(originalHelmet);
                    playerHelmets.remove(target.getUniqueId());
                }
                frozenPlayers.remove(target.getUniqueId());

                ILangService lang = ServiceRegistry.get(ILangService.class);
                target.sendMessage(lang.getMessage("commands.freeze.messages.unfreeze.target", "%player%", moderator.getName()));

                if (frozenPlayers.isEmpty()) stopEnforcement();

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
        if (frozenPlayers.isEmpty()) stopEnforcement();
    }

    public Map<UUID, Location> getFrozenPlayers() {
        return frozenPlayers;
    }
}
