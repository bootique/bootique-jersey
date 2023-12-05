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
import io.bootique.di.DIRuntimeException;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.logback.LogbackModule;
import org.junit.jupiter.api.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BQTest
public class HttpTargets_TrustStoresIT {

    private static final String CLIENT_TRUST_STORE = "classpath:io/bootique/jersey/client/testkeystore_default_password";

    @BQApp
    static final BQRuntime app = Bootique
            .app("-s", "-c", "classpath:io/bootique/jersey/client/TrustStoresIT_server.yml")
            .modules(JettyModule.class, JerseyModule.class, LogbackModule.class)
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .createRuntime();

    @BQTestTool
    final BQTestFactory clientFactory = new BQTestFactory();

    protected String serviceUrl() {
        return JettyTester.getUrl(app) + "/get";
    }

    @Test
    public void namedTrustStore() {

        HttpTargets targets = clientFactory.app()
                .modules(JerseyClientModule.class, LogbackModule.class)
                .property("bq.jerseyclient.trustStores.ts1.location", CLIENT_TRUST_STORE)
                .property("bq.jerseyclient.targets.t.url", serviceUrl())
                .property("bq.jerseyclient.targets.t.trustStore", "ts1")
                .createRuntime()
                .getInstance(HttpTargets.class);

        Response r = targets.newTarget("t").request().get();
        Resource.assertResponse(r);
    }

    @Test
    public void namedTrustStore_DerivedTarget() {

        HttpTargets targets = clientFactory.app()
                .modules(JerseyClientModule.class, LogbackModule.class)
                .property("bq.jerseyclient.trustStores.ts1.location", CLIENT_TRUST_STORE)
                .property("bq.jerseyclient.targets.t.url", serviceUrl())
                .property("bq.jerseyclient.targets.t.trustStore", "ts1")
                .createRuntime()
                .getInstance(HttpTargets.class);

        Response r = targets.newTarget("t").path("2").request().get();
        Resource.assertResponse2(r);
    }

    @Test
    public void namedTrustStore_InvalidRef() {

        assertThrows(DIRuntimeException.class, () ->
                clientFactory.app()
                        .modules(JerseyClientModule.class, LogbackModule.class)
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
