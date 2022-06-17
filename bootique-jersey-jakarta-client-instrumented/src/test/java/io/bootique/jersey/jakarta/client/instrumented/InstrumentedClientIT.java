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

package io.bootique.jersey.jakarta.client.instrumented;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.jakarta.JerseyModule;
import io.bootique.jersey.jakarta.client.HttpClientFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.connector.PortFinder;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.jetty.server.ServerHolder;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BQTest
public class InstrumentedClientIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime server = Bootique.app("--server")
            .modules(JettyModule.class, JerseyModule.class)
            .module(binder -> JerseyModule.extend(binder).addResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    BQRuntime client;

    private static String getUrlBadPort(String getUrl) {
        ServerHolder serverHolder = server.getInstance(ServerHolder.class);
        int goodPort = serverHolder.getConnector().getPort();
        int badPort = PortFinder.findAvailablePort(serverHolder.getConnector().getHost());
        return getUrl.replace(":" + goodPort, ":" + badPort);
    }

    @BeforeEach
    public void resetClient() {
        // important to recreate the client app before every test as it starts with zero metrics counters
        client = testFactory.app().autoLoadModules().createRuntime();
    }

    @Test
    public void testMetrics() {
        // fault filter to init metrics
        client.getInstance(ClientTimingFilter.class);
        MetricRegistry metricRegistry = client.getInstance(MetricRegistry.class);
        Set<String> expectedTimers = new HashSet<>(asList("bq.JerseyClient.Client.RequestTimer"));
        assertEquals(expectedTimers, metricRegistry.getTimers().keySet());
    }

    @Test
    public void testTimerInvoked() {

        HttpClientFactory factory = client.getInstance(HttpClientFactory.class);
        MetricRegistry metrics = client.getInstance(MetricRegistry.class);

        Collection<Timer> timers = metrics.getTimers().values();
        assertEquals(1, timers.size());
        Timer timer = timers.iterator().next();
        assertEquals(0, timer.getCount());

        factory.newClient().target(jetty.getUrl() + "/get").request().get().close();
        assertEquals(1, timer.getCount());

        factory.newClient().target(jetty.getUrl() + "/get").request().get().close();
        assertEquals(2, timer.getCount());
    }

    @Test
    public void testTimer_ConnectionError() {

        Client jaxrsClient = client.getInstance(HttpClientFactory.class).newClient();
        MetricRegistry metrics = client.getInstance(MetricRegistry.class);

        Collection<Timer> timers = metrics.getTimers().values();
        assertEquals(1, timers.size());
        Timer timer = timers.iterator().next();
        assertEquals(0, timer.getCount());

        // bad request
        String badPortUrl = getUrlBadPort(jetty.getUrl() + "/get");
        assertThrows(ProcessingException.class, () -> jaxrsClient.target(badPortUrl).request().get().close());
        assertEquals(0, timer.getCount());

        // successful request
        jaxrsClient.target(jetty.getUrl() + "/get").request().get().close();
        assertEquals(1, timer.getCount());
    }

    @Test
    public void testTimer_ServerErrors() {

        Client jaxrsClient = client.getInstance(HttpClientFactory.class).newClient();
        MetricRegistry metrics = client.getInstance(MetricRegistry.class);

        Collection<Timer> timers = metrics.getTimers().values();
        assertEquals(1, timers.size());
        Timer timer = timers.iterator().next();
        assertEquals(0, timer.getCount());

        jaxrsClient.target(jetty.getUrl() + "/get500").request().get().close();
        assertEquals(1, timer.getCount());
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
        @Path("get500")
        public Response get500() {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
