package io.bootique.jersey.client.instrumented.threshold;

import com.codahale.metrics.MetricRegistry;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.jersey.client.instrumented.RequestTimer;
import io.bootique.jersey.client.instrumented.JerseyClientInstrumentedModule;
import io.bootique.metrics.health.HealthCheck;
import io.bootique.metrics.health.check.DoubleRangeFactory;
import io.bootique.metrics.health.check.ValueRange;
import io.bootique.metrics.health.check.ValueRangeCheck;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@BQConfig
public class JerseyClientHealthChecksFactory {

    public static final String REQUESTS_PER_MIN_CHECK =  JerseyClientInstrumentedModule
            .METRIC_NAMING
            .name("Requests", "PerMin");

    private DoubleRangeFactory requestsPerMin;

    @BQConfigProperty
    public void setRequestsPerMin(DoubleRangeFactory requestsPerMin) {
        this.requestsPerMin = requestsPerMin;
    }

    public JerseyClientHealthChecks createHealthChecks(MetricRegistry metricRegistry) {
        return new JerseyClientHealthChecks(createHealthChecksMap(metricRegistry));
    }

    protected Map<String, HealthCheck> createHealthChecksMap(MetricRegistry registry) {
        Map<String, HealthCheck> checks = new HashMap<>(3);
        checks.put(REQUESTS_PER_MIN_CHECK, createTimeRequestsCheck(registry));
        return checks;
    }

    private HealthCheck createTimeRequestsCheck(MetricRegistry registry) {
        ValueRange<Double> range = getRequestsPerMin();
        Supplier<Double> deferredGauge = ()
                -> registry.timer(RequestTimer.TIMER_NAME).getOneMinuteRate();

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

        return ValueRange.builder(Double.class).min(0.0).warning(3.0).critical(15.0).build();
    }

}
