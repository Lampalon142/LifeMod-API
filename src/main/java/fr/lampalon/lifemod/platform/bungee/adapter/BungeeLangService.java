package fr.lampalon.lifemod.platform.bungee.adapter;

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
        return msg != null ? ChatColor.translateAlternateColorCodes('&', msg) : key;
    }

    @Override
    public String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace(placeholders[i], placeholders[i+1]);
            }
        }
        return msg;
    }
}
