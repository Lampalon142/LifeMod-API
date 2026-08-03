package fr.lampalon.lifemod.platform.bukkit.adapter;

import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import org.bukkit.configuration.file.FileConfiguration;

public class BukkitConfigurationService implements IConfigurationService {
    private final FileConfiguration config;

    public BukkitConfigurationService(FileConfiguration config) {
        this.config = config;
    }

    @Override
    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    @Override
    public java.util.List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return config.getInt(key, defaultValue);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return config.getDouble(key, defaultValue);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    @Override
    public String getPrefix() {
        return config.getString("prefix");
    }
}