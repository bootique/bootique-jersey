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

package io.bootique.jersey.v3.client;

import io.bootique.jersey.v3.client.DefaultHttpClientFactory;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class DefaultHttpClientFactoryTest {

    private ClientConfig config = new ClientConfig();
    private ClientRequestFilter mockAuth1 = mock(ClientRequestFilter.class);
    private ClientRequestFilter mockAuth2 = mock(ClientRequestFilter.class);

    @Test
    public void testNewClient() {

        config.property("x", "y");

        DefaultHttpClientFactory factory = new DefaultHttpClientFactory(config, Collections.emptyMap(), Collections.emptyMap());
        Client c = factory.newClient();
        assertNotNull(c);

        assertEquals("y", c.getConfiguration().getProperty("x"));
    }

    @Test
    public void testNewClient_NewInstanceEveryTime() {

        DefaultHttpClientFactory factory = new DefaultHttpClientFactory(
                config,
                Collections.emptyMap(),
                Collections.emptyMap());

        Client c1 = factory.newClient();
        Client c2 = factory.newClient();
        assertNotSame(c1, c2);
    }

    @Test
    public void testNewClientBuilder_Auth() {

        config.property("a", "b");

        Map<String, ClientRequestFilter> authFilters = new HashMap<>();
        authFilters.put("one", mockAuth1);
        authFilters.put("two", mockAuth2);

        DefaultHttpClientFactory factory = new DefaultHttpClientFactory(config, authFilters, Collections.emptyMap());
        Client c = factory.newBuilder().auth("one").build();
        assertNotNull(c);

        assertEquals("b", c.getConfiguration().getProperty("a"));
        assertTrue(c.getConfiguration().isRegistered(mockAuth1));
        assertFalse(c.getConfiguration().isRegistered(mockAuth2));
    }

    @Test
    public void testNewClient_Auth_BadAuth() {

        config.property("a", "b");

        Map<String, ClientRequestFilter> authFilters = new HashMap<>();
        authFilters.put("one", mockAuth1);
        authFilters.put("two", mockAuth2);

        DefaultHttpClientFactory factory = new DefaultHttpClientFactory(config, authFilters, Collections.emptyMap());
        assertThrows(IllegalArgumentException.class, () -> factory.newBuilder().auth("three"));
    }
}
