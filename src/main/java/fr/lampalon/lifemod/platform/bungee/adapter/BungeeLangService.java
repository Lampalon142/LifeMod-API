package fr.lampalon.lifemod.platform.bungee.adapter;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.util.List;

public class BungeeLangService implements ILangService {

    private final Configuration lang;

    public BungeeLangService(Configuration lang) {
        this.lang = lang;
    }

    @Override
    public String getMessage(String key) {
        String msg = lang.getString(key);
        if (msg == null) return key;
        return formatMessage(msg);
    }

    @Override
    public String getMessage(String key, String... placeholders) {
        String msg = lang.getString(key);
        if (msg == null) return key;

        if (placeholders != null && placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                if (value != null) {
                    msg = msg.replace(placeholder, value);
                }
            }
        }
        return formatMessage(msg);
    }

    @Override
    public Boolean getBoolean(String key) {
        return lang.getBoolean(key, false);
    }

    @Override
    public String formatMessage(String message) {
        if (message == null) return "";
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        String prefix = config != null ? config.getPrefix() : "";
        String formatted = message.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', formatted);
    }

    @Override
    public List<String> getStringList(String key) {
        List<String> list = lang.getStringList(key);
        if (list == null) return java.util.Collections.emptyList();
        return list.stream().map(this::formatMessage).toList();
    }

    @Override
    public String getPrefix() {
        return "system.prefix";
    }
}
