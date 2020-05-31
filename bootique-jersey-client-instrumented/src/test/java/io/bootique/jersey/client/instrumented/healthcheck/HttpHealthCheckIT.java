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

package io.bootique.jersey.client.instrumented.healthcheck;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jersey.client.instrumented.JerseyClientInstrumentedModule;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.connector.PortFinder;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckStatus;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class HttpHealthCheckIT {

    @BQApp
    static final BQRuntime server = Bootique.app("--server")
            .modules(JettyModule.class, JerseyClientInstrumentedModule.class, JerseyModule.class)
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .module(JettyTester.moduleReplacingConnectors())
            .createRuntime();

    private WebTarget target(String path) {
        return JettyTester.getTarget(server).path(path);
    }

    @Test
    public void testSafeCheck_NoConnection() {
        // access a port that is not in use
        int openPort = PortFinder.findAvailablePort("127.0.0.1");
        WebTarget target = ClientBuilder.newClient().target("http://127.0.0.1:" + openPort);
        HealthCheckOutcome outcome = HttpHealthCheck.viaHEAD(target).safeCheck();
        assertEquals(HealthCheckStatus.CRITICAL, outcome.getStatus());

        // there may be some variation in the error message, but we need to the the connection error
        assertTrue(outcome.getMessage().startsWith("Connection error: Connection refused"), outcome.getMessage());
    }

    @Test
    public void testSafeCheck_ViaGet_TempRedirect() {
        WebTarget target = target("moved");
        HealthCheckOutcome outcome = HttpHealthCheck.viaGET(target).safeCheck();
        assertEquals(HealthCheckStatus.OK, outcome.getStatus(), outcome.toString());
        assertNull(outcome.getMessage(), outcome.toString());
    }

    @Test
    public void testSafeCheck_ViaGet_TempRedirect_Overwritten() {

        // testing that while the initial target is setup to not follow redirects, health check still does.

        ClientConfig config = new ClientConfig().property(ClientProperties.FOLLOW_REDIRECTS, false);
        WebTarget target = ClientBuilder.newClient(config).target(JettyTester.getServerUrl(server) + "/moved");

        HealthCheckOutcome outcome = HttpHealthCheck.viaGET(target).safeCheck();
        assertEquals(HealthCheckStatus.OK, outcome.getStatus(), outcome.toString());
        assertNull(outcome.getMessage(), outcome.toString());
    }

    @Test
    public void testSafeCheck_ViaHead_NotFound() {
        WebTarget target = target("no_such_thing");
        HealthCheckOutcome outcome = HttpHealthCheck.viaHEAD(target).safeCheck();
        assertEquals(HealthCheckStatus.WARNING, outcome.getStatus());
        assertEquals("Health check is possibly misconfigured. Response status: 404", outcome.getMessage());
    }

    @Test
    public void testSafeCheck_ViaGet_ServerError() {
        WebTarget target = target("get500");
        HealthCheckOutcome outcome = HttpHealthCheck.viaHEAD(target).safeCheck();
        assertEquals(HealthCheckStatus.CRITICAL, outcome.getStatus());
        assertEquals("Remote API response status: 500", outcome.getMessage());
    }

    @Test
    public void testSafeCheck_ViaGet() {
        WebTarget target = target("get");
        HealthCheckOutcome outcome = HttpHealthCheck.viaGET(target).safeCheck();
        assertEquals(HealthCheckStatus.OK, outcome.getStatus(), outcome.toString());
    }

    @Test
    public void testSafeCheck_ViaHead() {
        // note that we don't need an explicit OPTIONS endpoint ...
        // Jersey server must provide an implicit behavior for a valid resource

        WebTarget target = target("get");
        HealthCheckOutcome outcome = HttpHealthCheck.viaHEAD(target).safeCheck();
        assertEquals(HealthCheckStatus.OK, outcome.getStatus(), outcome.toString());
    }

    @Test
    public void testSafeCheck_ViaOptions() {
        // note that we don't need an explicit HEAD endpoint ...
        // Jersey server must provide an implicit behavior for a valid resource
        WebTarget target = target("get");
        HealthCheckOutcome outcome = HttpHealthCheck.viaOPTIONS(target).safeCheck();
        assertEquals(HealthCheckStatus.OK, outcome.getStatus(), outcome.toString());
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
        @Path("moved")
        public Response getMoved() throws URISyntaxException {
            return Response.status(Response.Status.MOVED_PERMANENTLY).location(new URI(JettyTester.getServerUrl(server) + "/get")).build();
        }

        @GET
        @Path("get500")
        public Response get500() {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
