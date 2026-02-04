package fr.lampalon.lifemod.platform.bukkit.adapter;

import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;

public class BukkitLangService implements ILangService {
    private final FileConfiguration lang;

    public BukkitLangService(FileConfiguration lang) {
        this.lang = lang;
    }

    @Override
    public String getMessage(String key) {
        String message = lang.getString(key, key);
        return MessageUtil.formatMessage(message);
    }

    @Override
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }
}

