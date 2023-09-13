package io.bootique.jersey.client.instrumented.healthcheck;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.bootique.jersey.client.instrumented.RequestTimer;
import io.bootique.jersey.client.instrumented.threshold.JerseyClientHealthChecks;
import io.bootique.jersey.client.instrumented.threshold.JerseyClientHealthChecksFactory;
import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckRegistry;
import io.bootique.metrics.health.check.DoubleRangeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.bootique.jersey.client.instrumented.threshold.JerseyClientHealthChecksFactory.REQUESTS_PER_MIN_CHECK;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RangeHealthCheckTest {

    private MetricRegistry registry;
    private JerseyClientHealthChecksFactory healthCheckFactory;
    private HealthCheckRegistry healthCheckRegistry;

    @BeforeEach
    public void before() {
        this.registry = Mockito.mock(MetricRegistry.class);
        this.healthCheckFactory = new JerseyClientHealthChecksFactory();
        DoubleRangeFactory timeRequestsThresholds = new DoubleRangeFactory();
        timeRequestsThresholds.setCritical(0.05);
        timeRequestsThresholds.setWarning(0.01);

        healthCheckFactory.setRequestsPerMin(timeRequestsThresholds);

        Mockito.when(registry.timer(RequestTimer.TIMER_NAME)).thenReturn(Mockito.mock(Timer.class));

        JerseyClientHealthChecks rangeHealthCheck = healthCheckFactory.createHealthChecks(registry);
        this.healthCheckRegistry = new HealthCheckRegistry(rangeHealthCheck.getHealthChecks());
    }

    @Test
    public void testRange_1() {
        Timer timer = registry.timer(RequestTimer.TIMER_NAME);
        
        Mockito.when(timer.getOneMinuteRate()).thenReturn(0.009);
        HealthCheckOutcome outcome = healthCheckRegistry.runHealthCheck(REQUESTS_PER_MIN_CHECK);

        assertEquals(HealthCheckOutcome.ok().getStatus(), outcome.getStatus());
    }


    @Test
    public void testRange_2() {
        Timer timer = registry.timer(RequestTimer.TIMER_NAME);

        Mockito.when(timer.getOneMinuteRate()).thenReturn(0.03);
        HealthCheckOutcome range = healthCheckRegistry.runHealthCheck(REQUESTS_PER_MIN_CHECK);

        assertEquals(HealthCheckOutcome.warning().getStatus(), range.getStatus());
    }

    @Test
    public void testRange_3() {
        Timer timer = registry.timer(RequestTimer.TIMER_NAME);

        Mockito.when(timer.getOneMinuteRate()).thenReturn(0.06);
        HealthCheckOutcome range = healthCheckRegistry.runHealthCheck(REQUESTS_PER_MIN_CHECK);

        assertEquals(HealthCheckOutcome.critical().getStatus(), range.getStatus());
    }

}
