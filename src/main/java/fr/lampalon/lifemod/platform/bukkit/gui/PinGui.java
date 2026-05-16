package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
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

public class PinGui {

    private String currentPin = "";
    private final Player player;
    private final ModeratorAuthService authService;
    private final ModeratorSessionManager sessionManager;
    private final ILangService lang;
    private final boolean isRegistering;
    private Inventory inventory;

    public PinGui(Player player) {
        this.player = player;
        this.authService = LifeMod.getInstance().getModeratorAuthService();
        this.sessionManager = LifeMod.getInstance().getModeratorSessionManager();
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

        ItemStack border = InvHelper.createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        InvHelper.fillBorder(inventory, border);

        // Progress bar
        String progressEntered = lang.getMessage("auth.gui.progress-entered");
        String progressEmpty = lang.getMessage("auth.gui.progress-empty");
        
        StringBuilder progressBuilder = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            progressBuilder.append(i < currentPin.length() ? progressEntered : progressEmpty);
        }
        
        String progressName = lang.getMessage("auth.gui.progress-name", "%progress%", progressBuilder.toString());
        ItemStack progressItem = InvHelper.createItem(Material.PAPER, progressName);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, progressItem);
        }

        // Digits
        for (int i = 0; i <= 9; i++) {
            int slot = getSlotForDigit(i);
            String digitName = lang.getMessage("auth.gui.digit-name", "%digit%", String.valueOf(i));
            String[] digitLore = lang.getStringList("auth.gui.digit-lore").toArray(new String[0]);
            inventory.setItem(slot, InvHelper.createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, digitName, digitLore));
        }

        // Actions
        inventory.setItem(25, InvHelper.createItem(Material.ORANGE_STAINED_GLASS_PANE, 
                lang.getMessage("auth.gui.delete-name"), 
                lang.getStringList("auth.gui.delete-lore").toArray(new String[0])));
        
        inventory.setItem(43, InvHelper.createItem(Material.RED_STAINED_GLASS_PANE, 
                lang.getMessage("auth.gui.clear-name"), 
                lang.getStringList("auth.gui.clear-lore").toArray(new String[0])));
        
        inventory.setItem(52, InvHelper.createItem(Material.LIME_STAINED_GLASS_PANE, 
                lang.getMessage("auth.gui.validate-name"), 
                lang.getStringList("auth.gui.validate-lore").toArray(new String[0])));
        
        InvHelper.fill(inventory, border);
    }

    private int getSlotForDigit(int digit) {
        return switch (digit) {
            case 1 -> 19;
            case 2 -> 20;
            case 3 -> 21;
            case 4 -> 28;
            case 5 -> 29;
            case 6 -> 30;
            case 7 -> 37;
            case 8 -> 38;
            case 9 -> 39;
            case 0 -> 47;
            default -> -1;
        };
    }

    public void handleClick(int slot) {
        if (slot == 19) addDigit(1);
        else if (slot == 20) addDigit(2);
        else if (slot == 21) addDigit(3);
        else if (slot == 28) addDigit(4);
        else if (slot == 29) addDigit(5);
        else if (slot == 30) addDigit(6);
        else if (slot == 37) addDigit(7);
        else if (slot == 38) addDigit(8);
        else if (slot == 39) addDigit(9);
        else if (slot == 47) addDigit(0);
        else if (slot == 25) backspace();
        else if (slot == 43) clear();
        else if (slot == 52) validate();
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

        if (isRegistering) {
            authService.registerModerator(player.getUniqueId(), player.getName(), currentPin, player.getAddress().getAddress().getHostAddress());
            sessionManager.authenticate(player.getUniqueId());
            player.sendMessage(lang.getMessage("auth.registered"));
            player.closeInventory();
        } else {
            if (authService.checkPassword(player.getUniqueId(), currentPin)) {
                sessionManager.authenticate(player.getUniqueId());
                player.sendMessage(lang.getMessage("auth.login-success"));
                player.closeInventory();
            } else {
                int attemptsLeft = sessionManager.decrementAttempts(player.getUniqueId());
                if (attemptsLeft <= 0) {
                    sessionManager.lock(player.getUniqueId());
                    player.kickPlayer(lang.getMessage("auth.login-locked"));
                } else {
                    player.sendMessage(lang.getMessage("auth.login-failed", "%attempts%", String.valueOf(attemptsLeft)));
                    currentPin = "";
                    updateInventory();
                }
            }
        }
    }
}
