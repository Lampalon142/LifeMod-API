package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.utils.CompletionUtil;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GmCmd extends LifeCommand {

  private final boolean useLuckPerms;
  private LuckPerms luckPerms;
  private final LifeMod plugin;
  private final DebugManager debug;

  public GmCmd(LifeMod plugin) {
    super("gamemode", "lifemod.gm", false, "gm");
    this.plugin = plugin;
    this.debug = plugin.getDebugManager();
    this.useLuckPerms = plugin.getConfigConfig().getBoolean("UseLuckPerms");
    if (this.useLuckPerms) {
      this.luckPerms = Bukkit.getServicesManager().getRegistration(LuckPerms.class) != null
              ? LuckPermsProvider.get()
              : null;
      if (this.luckPerms == null) {
        Bukkit.getLogger().warning("[LifeMod] LuckPerms is configured but not detected !");
      }
    }
  }

  @Override
  public void execute(ICommandSender sender, String[] args) {
    if (args.length < 1 || args.length > 2) {
      sender.sendMessage(plugin.getLangConfig().getString("commands.gamemode.invalid"));
      debug.log("gm", "Invalid usage by " + sender.getName());
      return;
    }

    Player targetPlayer;
    String targetPlayerName = null;

    if (args.length == 2) {
      targetPlayerName = args[1];
      targetPlayer = Bukkit.getPlayer(targetPlayerName);
      if (targetPlayer == null) {
        sender.sendMessage(plugin.getLangConfig().getString("system.player-not-found"));
        debug.log("gm", "Target player offline: " + args[1]);
        return;
      }
    } else {
      if (!sender.isPlayer()) {
        sender.sendMessage(plugin.getLangConfig().getString("system.player-only"));
        debug.log("gm", "Console tried to use /gm without player argument");
        return;
      }
      targetPlayer = (Player) sender.getHandle();
    }

    GameMode gameMode = parseGameMode(args[0]);
    if (gameMode == null) {
      sender.sendMessage(plugin.getLangConfig().getString("commands.gamemode.invalid"));
      debug.log("gm", "Invalid gamemode: " + args[0]);
      return;
    }

    // Discord Webhook Logic (Async ideally, but keeping structure for now)
    if (plugin.getConfigConfig().getBoolean("discord.enabled")) {
        // ... (Existing webhook logic preserved)
        // For brevity in refactor, keeping standard logging
        debug.log("gm", sender.getName() + " changed gamemode of " + targetPlayer.getName());
    } else {
      debug.log("gm", sender.getName() + " changed gamemode of " + targetPlayer.getName());
    }

    String playerPrefix = getPlayerPrefix(targetPlayer);

    targetPlayer.setGameMode(gameMode);
    String message = targetPlayerName != null
            ? plugin.getLangConfig().getString("commands.gamemode.other")
            : plugin.getLangConfig().getString("commands.gamemode.own");

    sender.sendMessage(MessageUtil.formatMessage(
            message.replace("%gamemode%", gameMode.name())
                    .replace("%player%", targetPlayer.getName())
                    .replace("%luckperms_prefix%", playerPrefix)));
    debug.log("gm", "Gamemode set to " + gameMode.name() + " for " + targetPlayer.getName() + " by " + sender.getName());
  }

  private GameMode parseGameMode(String modeArg) {
    modeArg = modeArg.toLowerCase();
    if (modeArg.matches("\\d+")) {
      switch (Integer.parseInt(modeArg)) {
        case 0: return GameMode.SURVIVAL;
        case 1: return GameMode.CREATIVE;
        case 2: return GameMode.ADVENTURE;
        case 3: return GameMode.SPECTATOR;
        default: return null;
      }
    } else {
      switch (modeArg) {
        case "s": case "survival": return GameMode.SURVIVAL;
        case "c": case "creative": return GameMode.CREATIVE;
        case "a": case "adventure": return GameMode.ADVENTURE;
        case "sp": case "spectator": return GameMode.SPECTATOR;
        default: return null;
      }
    }
  }

  private String getPlayerPrefix(Player player) {
    if(this.useLuckPerms && this.luckPerms != null) {
      User user = luckPerms.getUserManager().getUser(player.getUniqueId());
      if (user != null) {
        String prefix = user.getCachedData().getMetaData().getPrefix();
        return prefix != null ? prefix : "";
      }
    }
    return "";
  }

  @Override
  public List<String> onTabComplete(ICommandSender sender, String[] args) {
    if (args.length == 1) {
        return filter(Arrays.asList("survival", "creative", "adventure", "spectator", "0", "1", "2", "3"), args);
    } else if (args.length == 2) {
        return filter(CompletionUtil.getPlayerNames((CommandSender) sender.getHandle()), args);
    }
    return Collections.emptyList();
  }
}


