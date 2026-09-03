package fr.lampalon.lifemod.api.metrics;

import java.util.Map;

public interface IMetricsService {

    boolean isEnabled();

    Map<String, Long> getSnapshot();
}
