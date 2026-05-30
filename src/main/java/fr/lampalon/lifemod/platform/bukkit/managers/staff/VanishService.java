package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.events.vanish.PlayerVanishStateChangeEvent;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishService implements IVanishService {

    private final LifeMod plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final NamespacedKey vanishKey;
    private final String METADATA_KEY = "vanished";

    public void handleQuit(UUID uuid) {
        vanishedPlayers.remove(uuid);
    }

    public VanishService(LifeMod plugin) {
        this.plugin = plugin;
        this.vanishKey = new NamespacedKey(plugin, "is_vanished");
    }

    @Override
    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    @Override
    public void setVanished(Player player, boolean vanished, boolean silent) {
        UUID uuid = player.getUniqueId();
        
        // Trigger Event
        PlayerVanishStateChangeEvent event = new PlayerVanishStateChangeEvent(player, vanished, silent);
        Bukkit.getPluginManager().callEvent(event);

        if (vanished) {
            enableVanish(player, silent);
        } else {
            disableVanish(player, silent);
        }
    }

    private void enableVanish(Player player, boolean silent) {
        vanishedPlayers.add(player.getUniqueId());
        
        // Internal state
        player.getPersistentDataContainer().set(vanishKey, PersistentDataType.BYTE, (byte) 1);
        player.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));

        // Hide logic
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("lifemod.vanish.see") && !online.equals(player)) {
                online.hidePlayer(plugin, player);
            }
        }

        // Silent attributes
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        player.setCollidable(config.getBoolean("modules.mod-mode.vanish.no-collision", true));
        player.setCanPickupItems(config.getBoolean("modules.mod-mode.vanish.no-pickup", true));
        if (config.getBoolean("modules.mod-mode.vanish.invisibility-effect", true)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
        }

        if (!silent) {
            broadcastFakeQuit(player);
        }
        
        plugin.getDebugManager().log("vanish", player.getName() + " is now vanished.");
    }

    private void disableVanish(Player player, boolean silent) {
        vanishedPlayers.remove(player.getUniqueId());
        
        player.getPersistentDataContainer().remove(vanishKey);
        player.removeMetadata(METADATA_KEY, plugin);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }

        player.setCollidable(true);
        player.setCanPickupItems(true);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        if (!silent) {
            broadcastFakeJoin(player);
        }

        plugin.getDebugManager().log("vanish", player.getName() + " is no longer vanished.");
    }

    @Override
    public void updateAllForPlayer(Player joiningPlayer) {
        // Hide existing vanished players from the new joiner
        for (UUID uuid : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null && !joiningPlayer.hasPermission("lifemod.vanish.see")) {
                joiningPlayer.hidePlayer(plugin, vanished);
            }
        }
    }

    @Override
    public Set<UUID> getVanishedPlayers() {
        return vanishedPlayers;
    }

    private void broadcastFakeQuit(Player player) {
        fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        String formatted = lang.getMessage("vanish.fake-quit", "%player%", player.getName());
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.hasPermission("lifemod.vanish.see"))
                .forEach(p -> p.sendMessage(formatted));
    }

    private void broadcastFakeJoin(Player player) {
        fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        String formatted = lang.getMessage("vanish.fake-join", "%player%", player.getName());
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.hasPermission("lifemod.vanish.see"))
                .forEach(p -> p.sendMessage(formatted));
    }
}
