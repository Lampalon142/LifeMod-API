package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.FreezeManager;
import fr.lampalon.lifemod.platform.bukkit.managers.PlayerManager;
import fr.lampalon.lifemod.platform.bukkit.managers.VanishedManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class ModItemsInteract implements Listener {

  private final DebugManager debug = LifeMod.getInstance().getDebugManager();
  private final HashMap<Player, Long> cooldowns = new HashMap<>();
  private final int freezeCooldownTime = 1000;
  private final HashMap<UUID, Long> vanishCooldowns = new HashMap<>();
  private final int vanishCooldownTime = 5000;
  private final HashMap<UUID, CPSData> cpsTests = new HashMap<>();
  private final int cpsTestDuration = LifeMod.getInstance().getConfigConfig().getInt("moderation-items.cps-tester.duration", 10);
  private final int cpsTestCooldown = LifeMod.getInstance().getConfigConfig().getInt("moderation-items.cps-tester.cooldown", 30);
  private final HashMap<UUID, Long> cpsCooldowns = new HashMap<>();

  @EventHandler
  public void onInteract(PlayerInteractEntityEvent e) {
    Player player = e.getPlayer();
    if (!PlayerManager.isInModerationMod(player)) return;
    if (!(e.getRightClicked() instanceof Player)) return;

    Player target = (Player) e.getRightClicked();
    e.setCancelled(true);

    Material itemType = player.getInventory().getItemInMainHand().getType();
    switch (itemType) {
      case PAPER:
        openTargetInventory(player, target);
        break;
      case PACKED_ICE:
        handleFreeze(player, target);
        break;
      case BLAZE_ROD:
        handleKill(player, target);
        break;
      case CLOCK:
        handleCPSTest(player, target);
        break;
      default:
        break;
    }
  }

  private void handleCPSTest(Player mod, Player target) {
    fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
    if (cpsCooldowns.containsKey(mod.getUniqueId())) {
      long last = cpsCooldowns.get(mod.getUniqueId());
      if (System.currentTimeMillis() - last < cpsTestCooldown * 1000) {
        mod.sendMessage(MessageUtil.formatMessage(lang.getMessage("mod.items.cooldown", "&cCooldown active...")));
        return;
      }
    }

    CPSData data = new CPSData();
    data.startTime = System.currentTimeMillis();
    data.target = target;
    cpsTests.put(mod.getUniqueId(), data);

    mod.sendMessage(MessageUtil.formatMessage(lang.getMessage("mod.items.cps-start", "&aCPS test started on &e%player%").replace("%player%", target.getName())));
    
    Bukkit.getScheduler().runTaskLater(LifeMod.getInstance(), () -> {
      CPSData result = cpsTests.remove(mod.getUniqueId());
      if (result != null) {
        int cps = (int) (result.clicks / cpsTestDuration);
        mod.sendMessage(MessageUtil.formatMessage(lang.getMessage("mod.items.cps-result", "&6CPS Result for &e%player% : &b%cps% CPS").replace("%player%", target.getName()).replace("%cps%", String.valueOf(cps))));
      }
    }, cpsTestDuration * 20L);

    cpsCooldowns.put(mod.getUniqueId(), System.currentTimeMillis());
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
    if (!PlayerManager.isInModerationMod(player)) return;
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) return;

    Material itemType = player.getInventory().getItemInMainHand().getType();
    switch (itemType) {
      case ENDER_PEARL:
        teleportRandomPlayer(player);
        break;
      case BLAZE_POWDER:
        toggleVanish(player);
        break;
      default:
        break;
    }
  }

  private void openTargetInventory(Player player, Player target) {
    player.openInventory(target.getInventory());
  }

  private void handleFreeze(Player player, Player target) {
    FreezeManager fm = LifeMod.getInstance().getFreezeManager();
    fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
    if (fm.isPlayerFrozen(target.getUniqueId())) {
      fm.unfreezePlayer(player, target);
      player.sendMessage(MessageUtil.formatMessage(lang.getMessage("mod.items.unfreeze", "&aYou unfroze &e%player%").replace("%player%", target.getName())));
    } else {
      fm.freezePlayer(player, target);
      player.sendMessage(MessageUtil.formatMessage(lang.getMessage("mod.items.freeze", "&cYou froze &e%player%").replace("%player%", target.getName())));
    }
  }

  private void handleKill(Player player, Player target) {
    target.setHealth(0);
    fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
    player.sendMessage(MessageUtil.formatMessage(lang.getMessage("mod.items.kill", "&cYou killed &e%player%").replace("%player%", target.getName())));
  }

  private void teleportRandomPlayer(Player player) {
    List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
    online.remove(player);
    if (online.isEmpty()) return;
    player.teleport(online.get(new Random().nextInt(online.size())).getLocation());
  }

  private void toggleVanish(Player player) {
    VanishedManager vm = LifeMod.getInstance().getPlayerManager();
    fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
    boolean vanished = !VanishedManager.isVanished(player);
    vm.setVanished(vanished, player);
    String msg = vanished ? lang.getMessage("mod.items.vanish-on", "&aVanish enabled") : lang.getMessage("mod.items.vanish-off", "&cVanish disabled");
    player.sendMessage(MessageUtil.formatMessage(msg));
  }

  private static class CPSData {
    int clicks = 0;
    long startTime;
    Player target;
  }
}
