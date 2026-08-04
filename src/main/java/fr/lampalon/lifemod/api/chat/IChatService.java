package fr.lampalon.lifemod.api.chat;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface IChatService {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    List<String> getBlacklist();

    boolean addBlacklist(String word);

    boolean removeBlacklist(String word);

    void awaitChatInput(UUID playerUuid, Consumer<String> callback);
}