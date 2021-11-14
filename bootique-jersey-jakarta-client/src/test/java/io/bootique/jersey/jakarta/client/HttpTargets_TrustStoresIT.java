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
import io.bootique.di.DIRuntimeException;
import io.bootique.jersey.jakarta.client.HttpTargets;
import io.bootique.jersey.jakarta.client.JerseyClientModuleProvider;
import io.bootique.jersey.jakarta.JerseyModule;
import io.bootique.jetty.v11.JettyModule;
import io.bootique.jetty.v11.junit5.JettyTester;
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
public class HttpTargets_TrustStoresIT {

    private static final String CLIENT_TRUST_STORE = "classpath:io/bootique/jersey/jakarta/client/testkeystore_default_password";

    @BQApp
    static final BQRuntime app = Bootique
            .app("-s", "-c", "classpath:io/bootique/jersey/jakarta/client/TrustStoresIT_server.yml")
            .modules(JettyModule.class, JerseyModule.class)
            .moduleProvider(new LogbackModuleProvider())
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .createRuntime();

    @BQTestTool
    final BQTestFactory clientFactory = new BQTestFactory();

    protected String serviceUrl() {
        return JettyTester.getUrl(app) + "/get";
    }

    @Test
    public void testNamedTrustStore() {

        HttpTargets targets = clientFactory.app()
                .moduleProvider(new JerseyClientModuleProvider())
                .moduleProvider(new LogbackModuleProvider())
                .property("bq.jerseyclient.trustStores.ts1.location", CLIENT_TRUST_STORE)
                .property("bq.jerseyclient.targets.t.url", serviceUrl())
                .property("bq.jerseyclient.targets.t.trustStore", "ts1")
                .createRuntime()
                .getInstance(HttpTargets.class);

        Response r = targets.newTarget("t").request().get();
        Resource.assertResponse(r);
    }

    @Test
    public void testNamedTrustStore_DerivedTarget() {

        HttpTargets targets = clientFactory.app()
                .moduleProvider(new JerseyClientModuleProvider())
                .moduleProvider(new LogbackModuleProvider())
                .property("bq.jerseyclient.trustStores.ts1.location", CLIENT_TRUST_STORE)
                .property("bq.jerseyclient.targets.t.url", serviceUrl())
                .property("bq.jerseyclient.targets.t.trustStore", "ts1")
                .createRuntime()
                .getInstance(HttpTargets.class);

        Response r = targets.newTarget("t").path("2").request().get();
        Resource.assertResponse2(r);
    }

    @Test
    public void testNamedTrustStore_InvalidRef() {

        assertThrows(DIRuntimeException.class, () ->
                clientFactory.app()
                        .moduleProvider(new JerseyClientModuleProvider())
                        .moduleProvider(new LogbackModuleProvider())
                        .property("bq.jerseyclient.targets.t.url", serviceUrl())
                        .property("bq.jerseyclient.targets.t.trustStore", "ts1")
                        .createRuntime()
                        .getInstance(HttpTargets.class));
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        static void assertResponse(Response response) {
            assertEquals(200, response.getStatus());
            assertEquals("got", response.readEntity(String.class));
        }

        static void assertResponse2(Response response) {
            assertEquals(200, response.getStatus());
            assertEquals("got2", response.readEntity(String.class));
        }

        @GET
        @Path("get")
        public String get() {
            return "got";
        }

        @GET
        @Path("get/2")
        public String get2() {
            return "got2";
        }
    }
}
