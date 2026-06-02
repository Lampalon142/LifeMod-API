package fr.lampalon.lifemod.platform.bukkit.adapter;

import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class BukkitLangService implements ILangService {
    private final FileConfiguration langConfig;

    public BukkitLangService(FileConfiguration langConfig) {
        this.langConfig = langConfig;
    }

    @Override
    public String getMessage(String key) {
        String message = langConfig.getString(key);
        return message != null ? MessageUtil.formatMessage(message) : key;
    }

    @Override
    public String getMessage(String key, String... placeholders) {
        String message = langConfig.getString(key);
        if (message == null) return key;

        if (placeholders != null && placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                if (value != null) {
                    message = message.replace(placeholder, value);
                }
            }
        }

        return MessageUtil.formatMessage(message);
    }

    @Override
    public Boolean getBoolean(String key) {
        return langConfig.getBoolean(key, false);
    }

    @Override
    public String formatMessage(String message) {
        return MessageUtil.formatMessage(message);
    }

    @Override
    public List<String> getStringList(String key) {
        List<String> list = langConfig.getStringList(key);
        if (list == null) return java.util.Collections.emptyList();
        
        return list.stream().map(MessageUtil::formatMessage).toList();
    }

    @Override
    public String getPrefix() {
        String prefix = langConfig.getString("system.prefix");
        return prefix != null ? prefix : "";
    }
}