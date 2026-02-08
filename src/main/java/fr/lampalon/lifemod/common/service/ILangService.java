package fr.lampalon.lifemod.common.service;

public interface ILangService {
    String getMessage(String key);
    String getMessage(String key, String... placeholders);
    java.util.List<String> getStringList(String key);
}


