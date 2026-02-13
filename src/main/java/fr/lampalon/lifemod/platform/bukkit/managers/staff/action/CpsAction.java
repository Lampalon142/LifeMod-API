package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CpsAction implements IStaffAction {

    private final Map<UUID, Long> activeTests = new HashMap<>();

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        player.sendMessage(MessageUtil.formatMessage("&cYou must click on a player to start a CPS test."));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player target = (Player) event.getRightClicked();

        if (activeTests.containsKey(player.getUniqueId())) {
            player.sendMessage(MessageUtil.formatMessage("&cYou already have an active CPS test."));
            return;
        }

        int duration = LifeMod.getInstance().getConfigConfig().getInt("modules.mod-mode.items.cpstester.duration", 10);
        
        player.sendMessage(MessageUtil.formatMessage("&aStarting &e" + duration + "s &aCPS test on &e" + target.getName() + "&a..."));
        activeTests.put(player.getUniqueId(), System.currentTimeMillis());

        // Snapshot of clicks at start
        long startTime = System.currentTimeMillis();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                activeTests.remove(player.getUniqueId());
                if (!player.isOnline()) return;

                List<Long> clicksDuringTest = getClicksInRange(target, startTime, System.currentTimeMillis());
                
                if (clicksDuringTest.isEmpty()) {
                    player.sendMessage(MessageUtil.formatMessage("&cTest finished for &e" + target.getName() + "&c: &60 CPS &7(No clicks detected)"));
                    return;
                }

                double cps = (double) clicksDuringTest.size() / duration;
                boolean suspectedAutoClicker = detectAutoClicker(clicksDuringTest);

                player.sendMessage(MessageUtil.formatMessage("&8&m----------------------------------------"));
                player.sendMessage(MessageUtil.formatMessage("&6&lCPS Test Result &7- &e" + target.getName()));
                player.sendMessage(MessageUtil.formatMessage(" "));
                player.sendMessage(MessageUtil.formatMessage("&e• &fAverage CPS: &b" + String.format("%.2f", cps)));
                player.sendMessage(MessageUtil.formatMessage("&e• &fTotal Clicks: &b" + clicksDuringTest.size()));
                player.sendMessage(MessageUtil.formatMessage("&e• &fAuto-Clicker: " + (suspectedAutoClicker ? "&c&lSUSPECTED" : "&aUnlikely")));
                player.sendMessage(MessageUtil.formatMessage(" "));
                player.sendMessage(MessageUtil.formatMessage("&8&m----------------------------------------"));
            }
        }.runTaskLater(LifeMod.getInstance(), duration * 20L);
    }

    private List<Long> getClicksInRange(Player target, long start, long end) {
        Map<UUID, Deque<Long>> map = LifeMod.getInstance().getCpsMap();
        Deque<Long> clicks = map.get(target.getUniqueId());
        if (clicks == null) return Collections.emptyList();

        List<Long> result = new ArrayList<>();
        synchronized (clicks) {
            for (Long time : clicks) {
                if (time >= start && time <= end) {
                    result.add(time);
                }
            }
        }
        return result;
    }

    private boolean detectAutoClicker(List<Long> clicks) {
        if (clicks.size() < 20) return false; // Not enough data

        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < clicks.size(); i++) {
            intervals.add(clicks.get(i) - clicks.get(i - 1));
        }

        // 1. Consistency check (Variance)
        long sum = 0;
        for (long interval : intervals) sum += interval;
        double mean = (double) sum / intervals.size();

        double varianceSum = 0;
        for (long interval : intervals) {
            varianceSum += Math.pow(interval - mean, 2);
        }
        double standardDeviation = Math.sqrt(varianceSum / intervals.size());

        // Human clicks usually have a standard deviation > 15-20ms. 
        // Auto-clickers are often < 5ms or even 0ms if fixed.
        if (standardDeviation < 8.0) return true;

        // 2. Pattern check: Too many identical intervals
        int identicalCount = 0;
        for (int i = 1; i < intervals.size(); i++) {
            if (Math.abs(intervals.get(i) - intervals.get(i - 1)) <= 1) {
                identicalCount++;
            }
        }

        double consistencyRatio = (double) identicalCount / intervals.size();
        return consistencyRatio > 0.8; // More than 80% identical intervals
    }
}
