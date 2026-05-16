package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.ActionBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FollowCommand extends LifeCommand {
    private final LifeMod plugin;
    private final Map<UUID, UUID> following = new HashMap<>(); // Follower UUID -> Target UUID
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>(); // Follower UUID -> Runnable Task

    public FollowCommand(LifeMod plugin) {
        super("follow", "lifemod.follow", true);
        this.plugin = plugin;
        setDescription("Follows a player, displaying their distance and CPS.");
        setUsage("/follow <player>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();

        if (args.length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.follow.usage"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        if (targetPlayer == player) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.follow.yourself"));
            return;
        }

        if (tasks.containsKey(player.getUniqueId())) {
            stopFollowing(player); // Stop current following if any
        }

        following.put(player.getUniqueId(), targetPlayer.getUniqueId());
        context.getSender().sendMessage(context.getLang().getMessage("commands.follow.success", "%target%", targetPlayer.getName()));
        context.getDebug().log("follow", player.getName() + " is now following " + targetPlayer.getName());

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !targetPlayer.isOnline() || !following.containsKey(player.getUniqueId())) {
                    stopFollowing(player);
                    this.cancel();
                    return;
                }
                
                double distance = player.getLocation().distance(targetPlayer.getLocation());
                int cps = getCPS(targetPlayer);

                String msg = context.getLang().getMessage("commands.follow.actionbar",
                        "%target%", targetPlayer.getName(),
                        "%distance%", String.format("%.1f", distance),
                        "%cps%", String.valueOf(cps));

                ActionBarUtil.sendActionBar(player, msg);
            }
        };
        task.runTaskTimer(plugin, 0L, 20L); // Every second
        tasks.put(player.getUniqueId(), task);
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }

    private int getCPS(Player player) {
        Deque<Long> deque = plugin.getCpsMap().get(player.getUniqueId());
        if (deque == null) return 0;
        long now = System.currentTimeMillis();
        // Remove clicks older than 1 second
        while (!deque.isEmpty() && now - deque.peekFirst() > 1000) {
            deque.pollFirst();
        }
        return deque.size();
    }

    public void stopFollowing(Player follower) {
        following.remove(follower.getUniqueId());
        if (tasks.containsKey(follower.getUniqueId())) {
            tasks.get(follower.getUniqueId()).cancel();
            tasks.remove(follower.getUniqueId());
            follower.sendMessage(ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class).getMessage("commands.follow.stopped"));
        }
    }
}
