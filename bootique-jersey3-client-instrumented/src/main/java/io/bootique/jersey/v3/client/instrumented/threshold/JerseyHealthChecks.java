package io.bootique.jersey.v3.client.instrumented.threshold;

import io.bootique.metrics.health.HealthCheck;
import io.bootique.metrics.health.HealthCheckGroup;

import java.util.Map;

public class JerseyHealthChecks implements HealthCheckGroup {
    private Map<String, HealthCheck> healthChecks;

    public JerseyHealthChecks(Map<String, HealthCheck> healthChecks) {
        this.healthChecks = healthChecks;
    }

    @Override
    public Map<String, HealthCheck> getHealthChecks() {
        return healthChecks;
    }
}
