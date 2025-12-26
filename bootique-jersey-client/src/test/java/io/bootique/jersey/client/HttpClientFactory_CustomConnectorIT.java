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
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.logback.LogbackModule;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.spi.Connector;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class HttpClientFactory_CustomConnectorIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime server = Bootique.app("--server")
            .modules(JettyModule.class, JerseyModule.class, LogbackModule.class)
            .module(b -> JerseyModule.extend(b).addApiResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @BQTestTool
    final BQTestFactory clientFactory = new BQTestFactory();

    @Test
    public void customConnector() {

        assertEquals(0, TestConnectorProvider.counter.get());

        HttpClientFactory factory =
                clientFactory.app()
                        .autoLoadModules()
                        .module(b -> JerseyClientModule.extend(b).setConnectorProvider(TestConnectorProvider.class))
                        .createRuntime()
                        .getInstance(HttpClientFactory.class);

        Client client = factory.newClient();

        Response r1 = client.target(JettyTester.getUrl(server)).path("get").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("got", r1.readEntity(String.class));
        assertEquals(1, TestConnectorProvider.counter.get());

        client.target(JettyTester.getUrl(server)).path("get").request().get();
        assertEquals(1, TestConnectorProvider.counter.get());
    }

    static class TestConnectorProvider extends HttpUrlConnectorProvider {

        static AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Connector getConnector(Client client, Configuration config) {
            counter.incrementAndGet();
            return super.getConnector(client, config);
        }
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        @Path("get")
        public String get() {
            return "got";
        }

        @GET
        @Path("get_auth")
        public String getAuth(@HeaderParam("Authorization") String auth) {
            return "got_" + auth;
        }
    }
}
