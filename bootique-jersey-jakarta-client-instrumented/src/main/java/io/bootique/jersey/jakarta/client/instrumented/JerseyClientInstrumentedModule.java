/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jersey.jakarta.client.instrumented;

import com.codahale.metrics.MetricRegistry;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jersey.jakarta.client.instrumented.threshold.JerseyHealthChecks;
import io.bootique.jersey.jakarta.client.instrumented.threshold.ThresholdHealthCheckFactory;
import io.bootique.jersey.client.JerseyClientModule;
import io.bootique.metrics.MetricNaming;
import io.bootique.metrics.health.HealthCheckModule;

import javax.inject.Singleton;

public class JerseyClientInstrumentedModule extends ConfigModule {

    public static final MetricNaming METRIC_NAMING = MetricNaming.forModule(JerseyClientInstrumentedModule.class);

    @Override
    public void configure(Binder binder) {

        JerseyClientModule.extend(binder).addFeature(InstrumentedFeature.class);
        HealthCheckModule.extend(binder).addHealthCheckGroup(JerseyHealthChecks.class);

    }

    @Provides
    @Singleton
    InstrumentedFeature provideTimingFeature(ClientTimingFilter filter) {
        return new InstrumentedFeature(filter);
    }

    @Provides
    @Singleton
    ClientTimingFilter provideTimingFilter(MetricRegistry metricRegistry) {
        return new ClientTimingFilter(metricRegistry);
    }

    @Provides
    ThresholdHealthCheckFactory providerThresholdHealthCheckFactory(ConfigurationFactory configFactory) {
        return config(ThresholdHealthCheckFactory.class, configFactory);
    }

    @Provides
    @Singleton
    JerseyHealthChecks provideThresholdHealthCheck(ThresholdHealthCheckFactory health, MetricRegistry metricRegistry) {
        return health.createThresholdHealthCheck(metricRegistry);
    }

}
