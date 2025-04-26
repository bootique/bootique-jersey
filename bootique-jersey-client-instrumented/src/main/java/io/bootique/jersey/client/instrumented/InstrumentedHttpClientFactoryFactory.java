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
package io.bootique.jersey.client.instrumented;

import com.codahale.metrics.MetricRegistry;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.di.Injector;
import io.bootique.jersey.client.HttpClientFactoryFactory;
import io.bootique.jersey.client.JerseyClientFeatures;
import io.bootique.jersey.client.instrumented.mdc.MDCAwareClientAsyncExecutorProvider;
import jakarta.ws.rs.core.Feature;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import jakarta.inject.Inject;
import java.util.Set;

/**
 * @since 3.0
 */
@BQConfig
public class InstrumentedHttpClientFactoryFactory extends HttpClientFactoryFactory {

    private final MetricRegistry metricRegistry;

    private JerseyClientHealthChecksFactory health;

    @Inject
    public InstrumentedHttpClientFactoryFactory(
            Injector injector,
            @JerseyClientFeatures Set<Feature> features,
            ConnectorProvider connectorProvider,
            MetricRegistry metricRegistry) {
        super(injector, features, connectorProvider);
        this.metricRegistry = metricRegistry;
    }

    @BQConfigProperty("Configures client healthcheck thresholds")
    public void setHealth(JerseyClientHealthChecksFactory health) {
        this.health = health;
    }

    public JerseyClientHealthChecks createHealthChecks() {
        return getHealth().createHealthChecks();
    }

    private JerseyClientHealthChecksFactory getHealth() {
        return health != null ? health : new JerseyClientHealthChecksFactory(metricRegistry);
    }

    @Override
    protected void configAsyncExecutor(ClientConfig config) {
        config.register(MDCAwareClientAsyncExecutorProvider.class);
    }
}
