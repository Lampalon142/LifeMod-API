package fr.lampalon.lifemod.platform.bungee.adapter;

import fr.lampalon.lifemod.common.service.IConfigurationService;
import net.md_5.bungee.config.Configuration;

public class BungeeConfigurationService implements IConfigurationService {

    private final Configuration config;

    public BungeeConfigurationService(Configuration config) {
        this.config = config;
    }

    @Override
    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
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
    public String getPrefix() {
        return config.getString("prefix", "");
    }
}
