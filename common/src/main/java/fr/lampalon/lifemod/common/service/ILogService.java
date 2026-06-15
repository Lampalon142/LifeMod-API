package fr.lampalon.lifemod.common.service;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogQuery;
import fr.lampalon.lifemod.common.model.LogType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ILogService {

    void log(LogEntry entry);

    default void log(LogType type, UUID playerUuid, String playerName, String actionData) {
        log(LogEntry.builder().type(type).playerUuid(playerUuid).playerName(playerName).actionData(actionData).now().build());
    }

    CompletableFuture<List<LogEntry>> query(LogQuery query);

    CompletableFuture<Long> count(LogQuery query);

    void flush();

    void shutdown();
}
