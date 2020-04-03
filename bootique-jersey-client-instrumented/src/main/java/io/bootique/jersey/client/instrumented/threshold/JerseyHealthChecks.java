package io.bootique.jersey.client.instrumented.threshold;

import io.bootique.metrics.health.HealthCheck;
import io.bootique.metrics.health.HealthCheckGroup;

import java.util.Map;

/**
 * @since 1.0.RC1
 */
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
