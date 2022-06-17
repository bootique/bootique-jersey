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

package io.bootique.jersey.jakarta.client;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.logback.LogbackModuleProvider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BQTest
public class HttpClientFactory_TrustStoresIT {

    @BQApp
    static final BQRuntime server = Bootique
            .app("-s", "-c", "classpath:io/bootique/jersey/jakarta/client/TrustStoresIT_server.yml")
            .modules(JettyModule.class, JerseyModule.class)
            .moduleProvider(new LogbackModuleProvider())
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .createRuntime();

    @BQTestTool
    final BQTestFactory clientFactory = new BQTestFactory();

    private String serviceUrl() {
        return JettyTester.getUrl(server) + "/get";
    }

    @Test
    public void testNamedTrustStore() {

        HttpClientFactory factory = clientFactory
                .app("-c", "classpath:io/bootique/jersey/jakarta/client/TrustStoresIT_client.yml")
                .moduleProvider(new JerseyClientModuleProvider())
                .moduleProvider(new LogbackModuleProvider())
                .createRuntime()
                .getInstance(HttpClientFactory.class);

        Response r1 = factory.newBuilder()
                .trustStore("t1")
                .build()
                .target(serviceUrl())
                .request()
                .get();

        Resource.assertResponse(r1);

        Response r2 = factory.newBuilder()
                .trustStore("t2_default_password")
                .build()
                .target(serviceUrl())
                .request()
                .get();

        Resource.assertResponse(r2);

    }

    @Test
    public void testNamedTrustStore_Invalid() {

        HttpClientFactory factory = clientFactory
                .app("-c", "classpath:io/bootique/jersey/jakarta/client/TrustStoresIT_client.yml")
                .moduleProvider(new JerseyClientModuleProvider())
                .moduleProvider(new LogbackModuleProvider())
                .createRuntime()
                .getInstance(HttpClientFactory.class);

        assertThrows(IllegalArgumentException.class, () -> factory.newBuilder().trustStore("no_such_name"));
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        static void assertResponse(Response response) {
            assertEquals(200, response.getStatus());
            assertEquals("got", response.readEntity(String.class));
        }

        @GET
        @Path("get")
        public String get() {
            return "got";
        }
    }
}
