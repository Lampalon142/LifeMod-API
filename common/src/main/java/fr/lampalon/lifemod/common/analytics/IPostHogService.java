package fr.lampalon.lifemod.common.analytics;

import java.util.Map;

public interface IPostHogService {

    boolean isEnabled();

    void capture(String eventName, Map<String, Object> properties);

    void capture(String eventName, String distinctId, Map<String, Object> properties);

    void shutdown();
}
