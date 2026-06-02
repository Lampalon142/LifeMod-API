package fr.lampalon.lifemod.common.core;

import java.util.HashMap;
import java.util.Map;

public class ServiceRegistry {
    private static final Map<Class<?>, Object> services = new HashMap<>();

    public static <T> void register(Class<T> serviceClass, T implementation) {
        services.put(serviceClass, implementation);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }
}

