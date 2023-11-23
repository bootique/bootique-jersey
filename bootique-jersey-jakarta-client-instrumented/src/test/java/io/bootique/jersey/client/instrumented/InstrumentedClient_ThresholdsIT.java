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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jersey.client.HttpClientFactory;
import io.bootique.jersey.client.JerseyClientModuleProvider;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckRegistry;
import io.bootique.metrics.health.HealthCheckStatus;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class InstrumentedClient_ThresholdsIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime server = Bootique
            .app("--server")
            .autoLoadModules()
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @BQTestTool
    public BQTestFactory clientFactory = new BQTestFactory();

    private BQRuntime createClient() {
        return clientFactory.app()
                .moduleProvider(new JerseyClientModuleProvider())
                .moduleProvider(new JerseyClientInstrumentedModuleProvider())
                .property("bq.jerseyclient.health.requestsPerMin.warning", "0.3")
                .property("bq.jerseyclient.health.requestsPerMin.critical", "0.6")
                .createRuntime();
    }

    private WebTarget createTarget(BQRuntime client) {
        return client
                .getInstance(HttpClientFactory.class)
                .newClient()
                .target(JettyTester.getUrl(server));
    }

    private HealthCheckOutcome runCheck(BQRuntime client, String checkName) {
        return client.getInstance(HealthCheckRegistry.class).runHealthCheck(checkName);
    }

    private HealthCheckOutcome tickAndRunCheck(BQRuntime client, String checkName) throws InterruptedException {

        // Timer/Meter "ticks" every 5 sec, so to see any value accumulation we have to wait 5 sec (making our tests rather slow).
        Thread.sleep(5001);
        return runCheck(client, checkName);
    }

    @Test
    public void requestsPerMin() throws InterruptedException {

        BQRuntime client = createClient();
        WebTarget target = createTarget(client);

        // no requests
        assertEquals(HealthCheckStatus.OK, runCheck(client, JerseyClientHealthChecksFactory.REQUESTS_PER_MIN_CHECK).getStatus());

        // 1 request
        JettyTester.assertOk(target.request().get());
        assertEquals(HealthCheckStatus.OK, tickAndRunCheck(client, JerseyClientHealthChecksFactory.REQUESTS_PER_MIN_CHECK).getStatus());

        // more requests
        // with exponentially decaying Meter, we need to load the system to trigger the thresholds
        for(int i = 0; i < 10; i++) {
            JettyTester.assertOk(target.request().get());
        }

        HealthCheckOutcome o1 = tickAndRunCheck(client, JerseyClientHealthChecksFactory.REQUESTS_PER_MIN_CHECK);
        assertEquals(HealthCheckStatus.WARNING, o1.getStatus(), () -> o1.toString());

        // even more requests
        for(int i = 0; i < 25; i++) {
            JettyTester.assertOk(target.request().get());
        }

        HealthCheckOutcome o2 = tickAndRunCheck(client, JerseyClientHealthChecksFactory.REQUESTS_PER_MIN_CHECK);
        assertEquals(HealthCheckStatus.CRITICAL, o2.getStatus(), () -> o2.toString());
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        public String hi() {
            return "hi";
        }
    }
}
