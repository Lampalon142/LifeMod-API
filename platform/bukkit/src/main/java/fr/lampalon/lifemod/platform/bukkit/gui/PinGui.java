package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorAuthService;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorSessionManager;
import fr.lampalon.lifemod.platform.bukkit.utils.InvHelper;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PinGui {

    private String currentPin = "";
    private final Player player;
    private final ModeratorAuthService authService;
    private final ModeratorSessionManager sessionManager;
    private final IConfigurationService config;
    private final ILangService lang;
    private final boolean isRegistering;
    private Inventory inventory;

    public PinGui(Player player) {
        this.player = player;
        this.authService = LifeMod.getInstance().getModeratorAuthService();
        this.sessionManager = LifeMod.getInstance().getModeratorSessionManager();
        this.config = ServiceRegistry.get(IConfigurationService.class);
        this.lang = ServiceRegistry.get(ILangService.class);
        this.isRegistering = !authService.isRegistered(player.getUniqueId());
    }

    public void open() {
        String titleKey = isRegistering ? "auth.gui.title-register" : "auth.gui.title-login";
        this.inventory = InvHelper.createInventory(6, lang.getMessage(titleKey));
        updateInventory();
        player.openInventory(inventory);
    }

    public void updateInventory() {
        inventory.clear();

        Material borderMat = Material.valueOf(config.getString("auth.gui.materials.border", "BLACK_STAINED_GLASS_PANE"));
        Material progressMat = Material.valueOf(config.getString("auth.gui.materials.progress", "PAPER"));
        Material digitMat = Material.valueOf(config.getString("auth.gui.materials.digit", "LIGHT_GRAY_STAINED_GLASS_PANE"));
        Material deleteMat = Material.valueOf(config.getString("auth.gui.materials.delete", "ORANGE_STAINED_GLASS_PANE"));
        Material clearMat = Material.valueOf(config.getString("auth.gui.materials.clear", "RED_STAINED_GLASS_PANE"));
        Material validateMat = Material.valueOf(config.getString("auth.gui.materials.validate", "LIME_STAINED_GLASS_PANE"));

        ItemStack border = InvHelper.createItem(borderMat, " ");
        InvHelper.fillBorder(inventory, border);

        // Progress bar
        String progressEntered = lang.getMessage("auth.gui.progress-entered");
        String progressEmpty = lang.getMessage("auth.gui.progress-empty");
        
        StringBuilder progressBuilder = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            progressBuilder.append(i < currentPin.length() ? progressEntered : progressEmpty);
        }
        
        String progressName = lang.getMessage("auth.gui.progress-name", "%progress%", progressBuilder.toString());
        ItemStack progressItem = InvHelper.createItem(progressMat, progressName);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, progressItem);
        }

        // Digits
        for (int i = 0; i <= 9; i++) {
            int slot = getSlotForDigit(i);
            String digitName = lang.getMessage("auth.gui.digit-name", "%digit%", String.valueOf(i));
            String[] digitLore = lang.getStringList("auth.gui.digit-lore").toArray(new String[0]);
            inventory.setItem(slot, InvHelper.createItem(digitMat, digitName, digitLore));
        }

        // Actions
        int deleteSlot = config.getInt("auth.gui.slots.delete", 25);
        int clearSlot = config.getInt("auth.gui.slots.clear", 43);
        int validateSlot = config.getInt("auth.gui.slots.validate", 52);

        inventory.setItem(deleteSlot, InvHelper.createItem(deleteMat, 
                lang.getMessage("auth.gui.delete-name"), 
                lang.getStringList("auth.gui.delete-lore").toArray(new String[0])));
        
        inventory.setItem(clearSlot, InvHelper.createItem(clearMat, 
                lang.getMessage("auth.gui.clear-name"), 
                lang.getStringList("auth.gui.clear-lore").toArray(new String[0])));
        
        inventory.setItem(validateSlot, InvHelper.createItem(validateMat, 
                lang.getMessage("auth.gui.validate-name"), 
                lang.getStringList("auth.gui.validate-lore").toArray(new String[0])));
        
        InvHelper.fill(inventory, border);
    }

    private int getSlotForDigit(int digit) {
        return switch (digit) {
            case 1 -> config.getInt("auth.gui.slots.digit-1", 19);
            case 2 -> config.getInt("auth.gui.slots.digit-2", 20);
            case 3 -> config.getInt("auth.gui.slots.digit-3", 21);
            case 4 -> config.getInt("auth.gui.slots.digit-4", 28);
            case 5 -> config.getInt("auth.gui.slots.digit-5", 29);
            case 6 -> config.getInt("auth.gui.slots.digit-6", 30);
            case 7 -> config.getInt("auth.gui.slots.digit-7", 37);
            case 8 -> config.getInt("auth.gui.slots.digit-8", 38);
            case 9 -> config.getInt("auth.gui.slots.digit-9", 39);
            case 0 -> config.getInt("auth.gui.slots.digit-0", 47);
            default -> -1;
        };
    }

    public void handleClick(int slot) {
        int d1 = config.getInt("auth.gui.slots.digit-1", 19);
        int d2 = config.getInt("auth.gui.slots.digit-2", 20);
        int d3 = config.getInt("auth.gui.slots.digit-3", 21);
        int d4 = config.getInt("auth.gui.slots.digit-4", 28);
        int d5 = config.getInt("auth.gui.slots.digit-5", 29);
        int d6 = config.getInt("auth.gui.slots.digit-6", 30);
        int d7 = config.getInt("auth.gui.slots.digit-7", 37);
        int d8 = config.getInt("auth.gui.slots.digit-8", 38);
        int d9 = config.getInt("auth.gui.slots.digit-9", 39);
        int d0 = config.getInt("auth.gui.slots.digit-0", 47);
        int deleteSlot = config.getInt("auth.gui.slots.delete", 25);
        int clearSlot = config.getInt("auth.gui.slots.clear", 43);
        int validateSlot = config.getInt("auth.gui.slots.validate", 52);

        if (slot == d1) addDigit(1);
        else if (slot == d2) addDigit(2);
        else if (slot == d3) addDigit(3);
        else if (slot == d4) addDigit(4);
        else if (slot == d5) addDigit(5);
        else if (slot == d6) addDigit(6);
        else if (slot == d7) addDigit(7);
        else if (slot == d8) addDigit(8);
        else if (slot == d9) addDigit(9);
        else if (slot == d0) addDigit(0);
        else if (slot == deleteSlot) backspace();
        else if (slot == clearSlot) clear();
        else if (slot == validateSlot) validate();
    }

    private void addDigit(int d) {
        if (currentPin.length() < 9) {
            currentPin += d;
            updateInventory();
        }
    }

    private void backspace() {
        if (!currentPin.isEmpty()) {
            currentPin = currentPin.substring(0, currentPin.length() - 1);
            updateInventory();
        }
    }

    private void clear() {
        currentPin = "";
        updateInventory();
    }

    private void validate() {
        if (currentPin.isEmpty()) return;

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);

        if (isRegistering) {
            String ip = player.getAddress().getAddress().getHostAddress();
            authService.registerModerator(player.getUniqueId(), player.getName(), currentPin, ip);
            sessionManager.authenticate(player.getUniqueId());
            authService.saveSession(player.getUniqueId(), ip);
            player.sendMessage(lang.getMessage("auth.registered"));
            player.closeInventory();

            if (ph != null) {
                Map<String, Object> props = new HashMap<>();
                props.put("action", "register");
                props.put("success", true);
                props.put("session_restored", false);
                ph.capture("lifemod_mod_auth", props);
            }
        } else {
            player.sendMessage(lang.getMessage("auth.verifying"));
            String pinToCheck = currentPin;
            currentPin = "";
            UUID uuid = player.getUniqueId();
            String playerName = player.getName();
            Player playerRef = player;
            authService.checkPasswordAsync(uuid, pinToCheck, success -> {
                if (!playerRef.isOnline()) return;
                if (success) {
                    sessionManager.authenticate(uuid);
                    authService.saveSession(uuid, playerRef.getAddress().getAddress().getHostAddress());
                    playerRef.sendMessage(lang.getMessage("auth.login-success"));
                    playerRef.closeInventory();

                    if (ph != null) {
                        Map<String, Object> props = new HashMap<>();
                        props.put("action", "login");
                        props.put("success", true);
                        props.put("session_restored", false);
                        ph.capture("lifemod_mod_auth", props);
                    }
                } else {
                    int attemptsLeft = sessionManager.decrementAttempts(uuid);
                    if (ph != null) {
                        Map<String, Object> failProps = new HashMap<>();
                        failProps.put("current_attempt", 1);
                        ph.capture("lifemod_mod_auth_fail", failProps);
                    }
                    if (attemptsLeft <= 0) {
                        sessionManager.lock(uuid);
                        playerRef.kickPlayer(lang.getMessage("auth.login-locked"));
                    } else {
                        playerRef.sendMessage(lang.getMessage("auth.login-failed", "%attempts%", String.valueOf(attemptsLeft)));
                        currentPin = "";
                        updateInventory();
                    }
                }
            });
        }
    }
}
