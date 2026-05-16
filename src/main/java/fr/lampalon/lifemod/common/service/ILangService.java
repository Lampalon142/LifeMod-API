package fr.lampalon.lifemod.common.service;

public interface ILangService {
    String getMessage(String key);
    String getMessage(String key, String... placeholders);
    String formatMessage(String message);
    java.util.List<String> getStringList(String key);
    String getPrefix();
}