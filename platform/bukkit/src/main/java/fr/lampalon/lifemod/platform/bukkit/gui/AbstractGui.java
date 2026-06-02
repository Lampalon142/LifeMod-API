package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.List;

public abstract class AbstractGui {

    protected final Player player;
    protected final IConfigurationService config;
    protected final ILangService lang;

    public AbstractGui(Player player) {
        this.player = player;
        this.config = ServiceRegistry.get(IConfigurationService.class);
        this.lang = ServiceRegistry.get(ILangService.class);
    }

    public abstract Gui buildGui();

    protected abstract String getTitleKey();

    protected String getTitle() {
        return lang.getMessage(getTitleKey());
    }

    public void open() {
        org.bukkit.Bukkit.getLogger().info("[DEBUG] AbstractGui.open() called for " + player.getName() + " with title " + getTitle());
        try {
            Window window = Window.single()
                    .setViewer(player)
                    .setTitle(getTitle())
                    .setGui(buildGui())
                    .build();

            org.bukkit.Bukkit.getLogger().info("[DEBUG] Window built, opening for " + player.getName());
            window.open();
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("[DEBUG] Failed to open GUI for " + player.getName());
            e.printStackTrace();
        }
    }

    protected Item createBorder() {
        String matName = config.getString("gui.border-material", "BLACK_STAINED_GLASS_PANE");
        return new SimpleItem(new ItemBuilder(Material.valueOf(matName)).setDisplayName(" ").setDamage((short) 15));
    }

    protected Item createBorder(Material material) {
        return new SimpleItem(new ItemBuilder(material).setDisplayName(" "));
    }

    protected Item createItem(Material material, String displayName, List<String> lore) {
        ItemBuilder builder = new ItemBuilder(material).setDisplayName(displayName);
        for (String line : lore) {
            builder.addLoreLines(line);
        }
        return new SimpleItem(builder);
    }

    protected Item createActionItem(Material material, String nameKey, String loreKey, String... placeholders) {
        String name = lang.getMessage(nameKey);
        String loreRaw = lang.getMessage(loreKey, placeholders);

        ItemBuilder builder = new ItemBuilder(material).setDisplayName(name);
        for (String line : loreRaw.split("\n")) {
            builder.addLoreLines(line);
        }
        
        return new SimpleItem(builder);
    }
}
