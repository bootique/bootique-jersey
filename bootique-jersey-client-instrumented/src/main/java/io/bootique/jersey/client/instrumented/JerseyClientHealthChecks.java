package io.bootique.jersey.client.instrumented;

import io.bootique.metrics.health.HealthCheck;
import io.bootique.metrics.health.HealthCheckGroup;

import java.util.Map;

public class JerseyClientHealthChecks implements HealthCheckGroup {
    private final Map<String, HealthCheck> healthChecks;

    public JerseyClientHealthChecks(Map<String, HealthCheck> healthChecks) {
        this.healthChecks = healthChecks;
    }

    @Override
    public Map<String, HealthCheck> getHealthChecks() {
        return healthChecks;
    }
}
