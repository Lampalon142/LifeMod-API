package fr.lampalon.lifemod.platform.bukkit.adapter;

import fr.lampalon.lifemod.common.service.ILangService;
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
        return message != null ? message : key;
    }

    @Override
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);

        if (message != null && placeholders != null && placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                if (value != null) {
                    message = message.replace(placeholder, value);
                }
            }
        }

        return message;
    }

    @Override
    public List<String> getStringList(String key) {
        List<String> list = langConfig.getStringList(key);
        return list != null ? list : java.util.Collections.emptyList();
    }
}