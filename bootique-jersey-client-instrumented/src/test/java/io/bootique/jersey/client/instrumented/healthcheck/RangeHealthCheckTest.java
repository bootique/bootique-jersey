package io.bootique.jersey.client.instrumented.healthcheck;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.bootique.jersey.client.instrumented.JerseyClientHealthChecks;
import io.bootique.jersey.client.instrumented.JerseyClientHealthChecksFactory;
import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckRegistry;
import io.bootique.metrics.health.check.DoubleRangeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.bootique.jersey.client.instrumented.JerseyClientHealthChecksFactory.REQUESTS_PER_MIN_CHECK;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RangeHealthCheckTest {

    private TestTimer timer;
    private JerseyClientHealthChecksFactory healthCheckFactory;
    private HealthCheckRegistry healthCheckRegistry;

    @BeforeEach
    public void before() {
        MetricRegistry registry = new MetricRegistry() {
            @Override
            public Timer timer(String name) {
                return timer;
            }
        };
        this.healthCheckFactory = new JerseyClientHealthChecksFactory(registry);
        DoubleRangeFactory timeRequestsThresholds = new DoubleRangeFactory();
        timeRequestsThresholds.setCritical(0.05);
        timeRequestsThresholds.setWarning(0.01);

        healthCheckFactory.setRequestsPerMin(timeRequestsThresholds);

        JerseyClientHealthChecks rangeHealthCheck = healthCheckFactory.createHealthChecks();
        this.healthCheckRegistry = new HealthCheckRegistry(rangeHealthCheck.getHealthChecks());
    }

    @Test
    public void range_1() {
        this.timer = new TestTimer(0.009);
        HealthCheckOutcome outcome = healthCheckRegistry.runHealthCheck(REQUESTS_PER_MIN_CHECK);

        assertEquals(HealthCheckOutcome.ok().getStatus(), outcome.getStatus());
    }


    @Test
    public void range_2() {
        this.timer = new TestTimer(0.03);
        HealthCheckOutcome range = healthCheckRegistry.runHealthCheck(REQUESTS_PER_MIN_CHECK);

        assertEquals(HealthCheckOutcome.warning().getStatus(), range.getStatus());
    }

    @Test
    public void range_3() {
        this.timer = new TestTimer(0.06);
        HealthCheckOutcome range = healthCheckRegistry.runHealthCheck(REQUESTS_PER_MIN_CHECK);

        assertEquals(HealthCheckOutcome.critical().getStatus(), range.getStatus());
    }

    private static class TestTimer extends Timer {
        private final double oneMinuteRate;

        public TestTimer(double oneMinuteRate) {
            super();
            this.oneMinuteRate = oneMinuteRate;
        }

        @Override
        public double getOneMinuteRate() {
            return oneMinuteRate;
        }
    }

}
