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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import io.bootique.config.ConfigurationFactory;
import io.bootique.config.PolymorphicConfiguration;
import io.bootique.config.TypesFactory;
import io.bootique.config.jackson.JsonNodeConfigurationFactory;
import io.bootique.jackson.DefaultJacksonService;
import io.bootique.jackson.JacksonService;
import io.bootique.jersey.client.auth.AuthenticatorFactory;
import io.bootique.jersey.client.auth.BasicAuthenticatorFactory;
import io.bootique.log.DefaultBootLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpClientFactoryFactory_ConfigIT {

    private TypesFactory<PolymorphicConfiguration> typesFactory;

    @BeforeEach
    public void before() {
        typesFactory = new TypesFactory<>(
                getClass().getClassLoader(),
                PolymorphicConfiguration.class,
                new DefaultBootLogger(true));
    }

    private ConfigurationFactory factory(String yaml) {

        // not using a mock; making sure all Jackson extensions are loaded
        JacksonService jacksonService = new DefaultJacksonService(typesFactory.getTypes());

        YAMLParser parser;
        try {
            parser = new YAMLFactory().createParser(yaml);
            JsonNode rootNode = jacksonService.newObjectMapper().readTree(parser);
            return new JsonNodeConfigurationFactory(rootNode, jacksonService.newObjectMapper());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testClientFlags() {
        HttpClientFactoryFactory factory = factory(
                "r:\n  followRedirects: true\n  connectTimeoutMs: 78\n  readTimeoutMs: 66\n  asyncThreadPoolSize: 44\n")
                .config(HttpClientFactoryFactory.class, "r");

        assertEquals(true, factory.followRedirects);
        assertEquals(78, factory.connectTimeoutMs);
        assertEquals(66, factory.readTimeoutMs);
        assertEquals(44, factory.asyncThreadPoolSize);
    }

    @Test
    public void testAuthTypes() {
        HttpClientFactoryFactory factory = factory(
                "r:\n  auth:\n    a1:\n      type: basic\n      username: u1\n      password: p1\n")
                .config(HttpClientFactoryFactory.class, "r");

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
