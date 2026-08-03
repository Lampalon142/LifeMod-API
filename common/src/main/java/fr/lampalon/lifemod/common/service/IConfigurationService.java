package fr.lampalon.lifemod.common.service;

import java.util.List;

public interface IConfigurationService {
    String getString(String key, String defaultValue);
    List<String>  getStringList(String path);
    boolean getBoolean(String key, boolean defaultValue);
    int getInt(String key, int defaultValue);
    double getDouble(String key, double defaultValue);
    long getLong(String key, long defaultValue);
    String getPrefix();
}


