package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.BukkitPlatform;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.IStaffAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.StaffActionType;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public class ScriptExecutor {

    private final StaffActionManager actionManager;
    private final DebugManager debug;

    public ScriptExecutor(StaffActionManager actionManager) {
        this.actionManager = actionManager;
        this.debug = LifeMod.getInstance().getDebugManager();
    }

    public void execute(StaffActionContext context, List<String> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            debug.log("staff", "ScriptExecutor: no scripts for clickType=" + context.getClickType());
            return;
        }

        for (String script : scripts) {
            executeScript(context, script);
        }
    }

    private void executeScript(StaffActionContext context, String script) {
        Player player = context.getPlayer();
        String upper = script.toUpperCase().trim();

        debug.log("staff", "executeScript: '" + script + "'");

        if (upper.startsWith("[PLAYER]")) {
            String cmd = script.substring(8).trim()
                    .replace("%player%", player.getName())
                    .replace("%player_name%", player.getName());
            debug.log("staff", "[PLAYER] cmd=" + cmd);
            player.performCommand(cmd);
            return;
        }

        if (upper.startsWith("[CONSOLE]")) {
            String cmd = script.substring(9).trim()
                    .replace("%player%", player.getName())
                    .replace("%player_name%", player.getName());
            Entity target = context.getTargetEntity();
            if (target instanceof Player) {
                cmd = cmd.replace("%target%", target.getName());
            }
            debug.log("staff", "[CONSOLE] cmd=" + cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            return;
        }

        if (upper.startsWith("[MESSAGE]")) {
            ILangService lang = ServiceRegistry.get(ILangService.class);
            String key = script.substring(9).trim();
            String msg = lang.getMessage(key);
            debug.log("staff", "[MESSAGE] key=" + key + " resolved=" + msg);
            player.sendMessage(msg);
            return;
        }

        if (upper.startsWith("[ACTIONBAR]")) {
            ILangService lang = ServiceRegistry.get(ILangService.class);
            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
            if (platform instanceof BukkitPlatform) {
                NMSProvider nms = ((BukkitPlatform) platform).getNmsProvider();
                if (nms != null) {
                    String key = script.substring(11).trim();
                    String msg = lang.getMessage(key);
                    debug.log("staff", "[ACTIONBAR] key=" + key);
                    nms.sendActionBar(player, msg);
                }
            }
            return;
        }

        if (upper.startsWith("[SOUND]")) {
            try {
                String soundName = script.substring(7).trim().toUpperCase();
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                debug.log("staff", "[SOUND] " + soundName);
                player.playSound(player.getLocation(), sound, 1f, 1f);
            } catch (Exception e) {
                debug.log("staff", "[SOUND] error: " + e.getMessage());
            }
            return;
        }

        if (upper.startsWith("[NATIVE]")) {
            String actionName = script.substring(8).trim().toUpperCase();
            debug.log("staff", "[NATIVE] " + actionName);
            StaffActionType type = StaffActionType.fromString(actionName);
            if (type != null) {
                executeNative(type, context);
            } else {
                debug.log("staff", "[NATIVE] UNRECOGNIZED: " + actionName);
            }
            return;
        }

        StaffActionType type = StaffActionType.fromString(script.trim());
        if (type != null) {
            debug.log("staff", "bare action resolved to " + type);
            executeNative(type, context);
        } else {
            debug.log("staff", "bare action UNRECOGNIZED: '" + script.trim() + "'");
        }
    }

    private void executeNative(StaffActionType type, StaffActionContext context) {
        IStaffAction action = actionManager.getAction(type);
        if (action == null) {
            debug.log("staff", "executeNative: no action registered for " + type);
            return;
        }
        debug.log("staff", "executeNative: " + type + " -> " + action.getClass().getSimpleName());
        action.execute(context);
    }
}
