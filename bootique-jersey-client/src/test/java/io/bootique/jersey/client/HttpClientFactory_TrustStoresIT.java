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

import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.JettyModule;
import io.bootique.logback.LogbackModuleProvider;
import io.bootique.test.junit.BQTestFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class HttpClientFactory_TrustStoresIT {

    // hostname must be 'localhost'... '127.0.0.1' will cause SSL errors
    private static final String SERVICE_URL = "https://localhost:14001/get";

    @ClassRule
    public static BQTestFactory SERVER_FACTORY = new BQTestFactory();

    @Rule
    public BQTestFactory clientFactory = new BQTestFactory();

    @BeforeClass
    public static void beforeClass() {
        SERVER_FACTORY.app("-s", "-c", "classpath:io/bootique/jersey/client/TrustStoresIT_server.yml")
                .modules(JettyModule.class, JerseyModule.class)
                .module(new LogbackModuleProvider())
                .module(b -> JerseyModule.extend(b).addResource(Resource.class))
                .run();
    }

    @Test
    public void testNamedTrustStore() {

        HttpClientFactory factory = clientFactory
                .app("-c", "classpath:io/bootique/jersey/client/TrustStoresIT_client.yml")
                .module(new JerseyClientModuleProvider())
                .module(new LogbackModuleProvider())
                .createRuntime()
                .getInstance(HttpClientFactory.class);

        Response r1 = factory.newBuilder()
                .trustStore("t1")
                .build()
                .target(SERVICE_URL)
                .request()
                .get();

        Resource.assertResponse(r1);

        Response r2 = factory.newBuilder()
                .trustStore("t2_default_password")
                .build()
                .target(SERVICE_URL)
                .request()
                .get();

        Resource.assertResponse(r2);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testNamedTrustStore_Invalid() {

        HttpClientFactory factory = clientFactory
                .app("-c", "classpath:io/bootique/jersey/client/TrustStoresIT_client.yml")
                .module(new JerseyClientModuleProvider())
                .module(new LogbackModuleProvider())
                .createRuntime()
                .getInstance(HttpClientFactory.class);

        factory.newBuilder().trustStore("no_such_name");
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
