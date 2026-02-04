package fr.lampalon.lifemod.common.messaging;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RedisMessagingService implements IMessagingService {

    private final JedisPool jedisPool;
    private final String password;

    public RedisMessagingService(String host, int port, String password) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        
        this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
        this.password = password;
    }

    @Override
    public void publish(String channel, String message) {
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channel, message);
            }
        });
    }

    @Override
    public void subscribe(String channel, Consumer<String> handler) {
        // Le l'abonnement Redis est bloquant, on le lance dans un thread séparé
        Thread subscribeThread = new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        handler.accept(message);
                    }
                }, channel);
            }
        }, "LifeMod-Redis-Subscriber-" + channel);
        
        subscribeThread.setDaemon(true);
        subscribeThread.start();
    }

    @Override
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}

