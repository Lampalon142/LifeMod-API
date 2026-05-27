package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class SpectateManager {
    private final Map<UUID, Location> originalLocations = new HashMap<>();
    private final Map<UUID, UUID> lastTargets = new HashMap<>();
    private final Set<UUID> isFreecam = new HashSet<>();
    private final Map<UUID, UUID> spectateTarget = new HashMap<>();

    private String getLang(String key, String... placeholders) {
        return fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class).getMessage(key, placeholders);
    }

    public void startSpectate(Player staff, Player target) {
        if (staff.equals(target)) {
            staff.sendMessage(getLang("commands.spectate.self"));
            return;
        }
        if (isSpectating(staff)) {
            staff.sendMessage(getLang("commands.spectate.already"));
            return;
        }
        originalLocations.put(staff.getUniqueId(), staff.getLocation());
        UUID previousTarget = spectateTarget.get(staff.getUniqueId());
        if (previousTarget != null) {
            lastTargets.put(staff.getUniqueId(), previousTarget);
        }

        spectateTarget.put(staff.getUniqueId(), target.getUniqueId());

        staff.setGameMode(GameMode.SPECTATOR);
        staff.setSpectatorTarget(target);
        staff.sendMessage(getLang("commands.spectate.spectate-start", "%target%", target.getName()));
    }

    public void startFreecam(Player staff) {
        if (isSpectating(staff) && !isFreecam.contains(staff.getUniqueId())) {
            isFreecam.add(staff.getUniqueId());
            staff.setSpectatorTarget(null);
            staff.sendMessage(getLang("commands.spectate.freecam-start"));
            return;
        }
        if (!isSpectating(staff)) {
            originalLocations.put(staff.getUniqueId(), staff.getLocation());
        }
        isFreecam.add(staff.getUniqueId());
        spectateTarget.remove(staff.getUniqueId());
        staff.setGameMode(GameMode.SPECTATOR);
        staff.setSpectatorTarget(null);
        staff.sendMessage(getLang("commands.spectate.freecam-start"));
    }

    public void leaveSpectate(Player staff) {
        if (!isSpectating(staff)) {
            staff.sendMessage(getLang("commands.spectate.not-spectating"));
            return;
        }

        staff.setSpectatorTarget(null);

        Location originalLocation = originalLocations.remove(staff.getUniqueId());
        isFreecam.remove(staff.getUniqueId());
        spectateTarget.remove(staff.getUniqueId());

        staff.setGameMode(GameMode.SURVIVAL);

        if (originalLocation != null && originalLocation.getWorld() != null) {
            staff.teleport(originalLocation);
            staff.sendMessage(getLang("commands.spectate.spectate-leave"));
        } else {
            staff.sendMessage(getLang("commands.spectate.spectate-leave-no-pos"));
        }
    }

    public void spectateRandom(Player spectator) {
        List<Player> candidates = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(spectator) && !p.hasPermission("lifemod.staff")) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) {
            spectator.sendMessage(getLang("commands.spectate.random-error"));
            return;
        }
        Player target = candidates.get(new Random().nextInt(candidates.size()));
        startSpectate(spectator, target);
        spectator.sendMessage(getLang("commands.spectate.random-success", "%target%", target.getName()));
    }

    public void spectateBack(Player spectator) {
        UUID last = lastTargets.get(spectator.getUniqueId());
        if (last == null) {
            spectator.sendMessage(getLang("commands.spectate.back-error"));
            return;
        }
        Player target = Bukkit.getPlayer(last);
        if (target == null || !target.isOnline()) {
            spectator.sendMessage(getLang("commands.spectate.player-not-found", "%target%", "last"));
            return;
        }
        startSpectate(spectator, target);
        spectator.sendMessage(getLang("commands.spectate.back-success", "%target%", target.getName()));
    }

    public void sendPlayerList(Player spectator) {
        spectator.sendMessage(getLang("commands.spectate.list-header"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(spectator)) {
                String display = getLang("commands.spectate.list-player", "%player%", p.getName());
                net.md_5.bungee.api.chat.TextComponent msg =
                        new net.md_5.bungee.api.chat.TextComponent(display);
                msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/spectate " + p.getName()));
                
                String hoverText = getLang("commands.spectate.list-hover", "%player%", p.getName());
                msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.ComponentBuilder(hoverText).create()));
                spectator.spigot().sendMessage(msg);
            }
        }
    }

    public boolean isSpectating(Player player) {
        return originalLocations.containsKey(player.getUniqueId());
    }

    public void handleQuit(UUID playerId) {
        originalLocations.remove(playerId);
        lastTargets.remove(playerId);
        isFreecam.remove(playerId);
        spectateTarget.remove(playerId);
    }

    public boolean isFreecam(Player player) {
        return isFreecam.contains(player.getUniqueId());
    }
}

