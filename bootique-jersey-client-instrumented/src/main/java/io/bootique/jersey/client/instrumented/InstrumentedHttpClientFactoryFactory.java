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
import io.bootique.jersey.client.HttpClientFactoryFactory;
import io.bootique.jersey.client.instrumented.threshold.JerseyClientHealthChecks;
import io.bootique.jersey.client.instrumented.threshold.JerseyClientHealthChecksFactory;

/**
 * @since 3.0
 */
@BQConfig
public class InstrumentedHttpClientFactoryFactory extends HttpClientFactoryFactory {

    private JerseyClientHealthChecksFactory health;

    @BQConfigProperty("Configures client healthcheck thresholds")
    public void setHealth(JerseyClientHealthChecksFactory health) {
        this.health = health;
    }

    public JerseyClientHealthChecks createHealthChecks(MetricRegistry metricRegistry) {
        return getHealth().createHealthChecks(metricRegistry);
    }

    private JerseyClientHealthChecksFactory getHealth() {
        return health != null ? health : new JerseyClientHealthChecksFactory();
    }

}
