package fr.lampalon.lifemod.common.service;

public interface IConfigurationService {
    String getString(String key, String defaultValue);
    boolean getBoolean(String key, boolean defaultValue);
    int getInt(String key, int defaultValue);
    double getDouble(String key, double defaultValue);
    String getPrefix();
}


