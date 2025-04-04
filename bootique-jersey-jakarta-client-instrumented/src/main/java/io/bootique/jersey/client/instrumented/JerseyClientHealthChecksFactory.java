package io.bootique.jersey.client.instrumented;

import com.codahale.metrics.MetricRegistry;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.metrics.health.HealthCheck;
import io.bootique.metrics.health.check.DoubleRangeFactory;
import io.bootique.metrics.health.check.ValueRange;
import io.bootique.metrics.health.check.ValueRangeCheck;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@BQConfig
public class JerseyClientHealthChecksFactory {

    public static final String REQUESTS_PER_MIN_CHECK = JerseyClientInstrumentedModule
            .METRIC_NAMING
            .name("Requests", "PerMin");

    private final MetricRegistry metricRegistry;

    private DoubleRangeFactory requestsPerMin;

    @Inject
    public JerseyClientHealthChecksFactory(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @BQConfigProperty
    public void setRequestsPerMin(DoubleRangeFactory requestsPerMin) {
        this.requestsPerMin = requestsPerMin;
    }

    public JerseyClientHealthChecks createHealthChecks() {
        return new JerseyClientHealthChecks(createHealthChecksMap());
    }

    protected Map<String, HealthCheck> createHealthChecksMap() {
        Map<String, HealthCheck> checks = new HashMap<>(3);
        checks.put(REQUESTS_PER_MIN_CHECK, createTimeRequestsCheck());
        return checks;
    }

    private HealthCheck createTimeRequestsCheck() {
        ValueRange<Double> range = getRequestsPerMin();
        Supplier<Double> deferredGauge = ()
                -> metricRegistry.timer(RequestTimer.TIMER_NAME).getOneMinuteRate();

        return new ValueRangeCheck<>(range, deferredGauge);
    }

    protected ValueRange<Double> getRequestsPerMin() {

        // init min if it wasn't set...
        if (requestsPerMin != null) {
            if (requestsPerMin.getMin() == null) {
                requestsPerMin.setMin(0);
            }

            return requestsPerMin.createRange();
        }

        return ValueRange.builder(Double.class).min(0.0)
                .warning(60.0)
                .critical(120.0)
                .build();
    }

}
