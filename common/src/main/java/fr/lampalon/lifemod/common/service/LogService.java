package fr.lampalon.lifemod.common.service;

import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogService implements ILogService {

    private static final int MAX_BATCH = 500;

    private final DatabaseProvider db;
    private final IConfigurationService config;
    private final BlockingQueue<LogEntry> queue = new LinkedBlockingQueue<>();
    private final ExecutorService flushExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LifeMod-LogFlusher");
        t.setDaemon(true);
        return t;
    });
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LifeMod-LogScheduler");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService asyncExecutor;
    private volatile boolean running = true;

    public LogService(DatabaseProvider db, IConfigurationService config, ExecutorService asyncExecutor) {
        this.db = db;
        this.config = config;
        this.asyncExecutor = asyncExecutor;

        int flushInterval = config != null ? config.getInt("logs.flush-interval", 5) : 5;
        scheduler.scheduleAtFixedRate(this::flush, flushInterval, flushInterval, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::purgeTask, 1, 24, TimeUnit.HOURS);
    }

    @Override
    public void log(LogEntry entry) {
        if (!running) return;
        queue.offer(entry);
    }

    @Override
    public void flush() {
        if (!running) return;
        try {
            List<LogEntry> batch = new ArrayList<>(MAX_BATCH);
            queue.drainTo(batch, MAX_BATCH);
            if (!batch.isEmpty()) {
                db.saveLogBatch(batch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<List<LogEntry>> query(LogQuery query) {
        return CompletableFuture.supplyAsync(() -> db.queryLogs(query), asyncExecutor != null ? asyncExecutor : flushExecutor);
    }

    @Override
    public CompletableFuture<Long> count(LogQuery query) {
        return CompletableFuture.supplyAsync(() -> db.countLogs(query), asyncExecutor != null ? asyncExecutor : flushExecutor);
    }

    private void purgeTask() {
        try {
            if (config == null) {
                long defaultRetention = 86400000L * 30;
                db.purgeLogs(new HashMap<>(), defaultRetention);
                return;
            }
            Map<Integer, Long> perType = new HashMap<>();
            java.util.List<String> retentionList = config.getStringList("logs.retention.per-type");
            if (retentionList != null) for (String key : retentionList) {
                String[] parts = key.split(":", 2);
                if (parts.length == 2) {
                    try {
                        int typeOrdinal = Integer.parseInt(parts[0].trim());
                        long ms = parseDuration(parts[1].trim());
                        perType.put(typeOrdinal, ms);
                    } catch (NumberFormatException e) {
                        java.util.logging.Logger.getLogger(getClass().getName()).warning("Invalid log retention config: " + key);
                    }
                }
            }
            long defaultRetention = parseDuration(config.getString("logs.retention.default", "30d"));
            db.purgeLogs(perType, defaultRetention);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long parseDuration(String s) {
        if (s.equals("0") || s.equalsIgnoreCase("forever")) return 0;
        try {
            if (s.endsWith("d")) return Long.parseLong(s.substring(0, s.length() - 1)) * 86400000L;
            if (s.endsWith("h")) return Long.parseLong(s.substring(0, s.length() - 1)) * 3600000L;
            if (s.endsWith("m")) return Long.parseLong(s.substring(0, s.length() - 1)) * 60000L;
            return Long.parseLong(s) * 86400000L;
        } catch (NumberFormatException e) {
            return 86400000L * 30;
        }
    }

    @Override
    public void shutdown() {
        running = false;
        flush();
        scheduler.shutdown();
        flushExecutor.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
            flushExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
