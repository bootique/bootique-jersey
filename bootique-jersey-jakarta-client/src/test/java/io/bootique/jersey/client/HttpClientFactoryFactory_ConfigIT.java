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

package io.bootique.jersey.client;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jersey.client.auth.AuthenticatorFactory;
import io.bootique.jersey.client.auth.BasicAuthenticatorFactory;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class HttpClientFactoryFactory_ConfigIT {

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique
            .app("-c", "classpath:io/bootique/jersey/client/HttpClientFactoryFactory_ConfigIT.yml")
            .autoLoadModules()
            .createRuntime();

    @Test
    public void clientFlags() {

        HttpClientFactoryFactory factory = app
                .getInstance(ConfigurationFactory.class)
                .config(HttpClientFactoryFactory.class, "a");

        assertEquals(true, factory.followRedirects);
        assertEquals(78, factory.connectTimeoutMs);
        assertEquals(66, factory.readTimeoutMs);
        assertEquals(44, factory.asyncThreadPoolSize);
    }

    @Test
    public void authTypes() {
        HttpClientFactoryFactory factory = app
                .getInstance(ConfigurationFactory.class)
                .config(HttpClientFactoryFactory.class, "b");

        assertNotNull(factory.auth);
        assertEquals(1, factory.auth.size());

        AuthenticatorFactory authFactory = factory.auth.get("a1");
        assertNotNull(authFactory);
        assertTrue(authFactory instanceof BasicAuthenticatorFactory);

        BasicAuthenticatorFactory basicAuthFactory = (BasicAuthenticatorFactory) authFactory;
        assertEquals("u1", basicAuthFactory.getUsername());
        assertEquals("p1", basicAuthFactory.getPassword());
    }
}
