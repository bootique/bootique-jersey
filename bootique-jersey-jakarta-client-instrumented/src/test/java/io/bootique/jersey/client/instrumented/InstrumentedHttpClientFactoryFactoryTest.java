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
import io.bootique.di.Injector;
import io.bootique.jersey.client.ClientAsyncExecutorProvider;
import io.bootique.jersey.client.instrumented.mdc.MDCAwareClientAsyncExecutorProvider;
import jakarta.ws.rs.client.Client;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class InstrumentedHttpClientFactoryFactoryTest {

    private final Injector mockInjector = mock(Injector.class);

    @Test
    public void createClientFactory_AsyncThreadPool() {

        Client client = new InstrumentedHttpClientFactoryFactory(
                mockInjector,
                Set.of(),
                new HttpUrlConnectorProvider(),
                mock(MetricRegistry.class)).createClientFactory().newClient();

        try {
            assertTrue(client.getConfiguration().isRegistered(MDCAwareClientAsyncExecutorProvider.class));
            assertFalse(client.getConfiguration().isRegistered(ClientAsyncExecutorProvider.class));
        } finally {
            client.close();
        }
    }

}
