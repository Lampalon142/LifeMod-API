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
        Window window = Window.single()
                .setViewer(player)
                .setTitle(getTitle())
                .setGui(buildGui())
                .build();
        
        window.open();
    }

    protected Item createBorder() {
        return new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ").setDamage((short) 15)); // Black stained glass for 1.8-1.12, or just DARK_GRAY in newer
    }

    protected Item createBorder(Material material) {
        return new SimpleItem(new ItemBuilder(material).setDisplayName(" "));
    }

    protected Item createItem(Material material, String displayName, List<String> lore) {
        ItemBuilder builder = new ItemBuilder(material).setDisplayName(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(displayName));
        for (String line : lore) {
            builder.addLoreLines(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(line));
        }
        return new SimpleItem(builder);
    }

    protected Item createActionItem(Material material, String nameKey, String loreKey, String... placeholders) {
        String name = lang.getMessage(nameKey);
        String loreRaw = lang.getMessage(loreKey);
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                loreRaw = loreRaw.replace(placeholders[i], placeholders[i+1]);
            }
        }

        ItemBuilder builder = new ItemBuilder(material).setDisplayName(name);
        for (String line : loreRaw.split("\n")) {
            builder.addLoreLines(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(line));
        }
        
        return new SimpleItem(builder);
    }
}
