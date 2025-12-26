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

package io.bootique.jersey;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class ResourceInjectionIT {

    private static final String TEST_PROPERTY = "bq.test.label";
    private static final InjectedService SERVICE = new InjectedService();

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> b.bind(InjectedService.class).toInstance(SERVICE))
            .module(b -> b.bind(ProviderManagedApi.class).toProviderInstance(() -> new ProviderManagedApi(SERVICE)))
            .module(b -> JerseyModule.extend(b)
                    .setProperty(TEST_PROPERTY, "x")
                    .addApiResource(FieldInjectedApi.class)
                    .addApiResource(FieldInjectedSingletonApi.class)
                    .addApiResource(ConstructorInjectedApi.class)
                    .addApiResource(ProviderManagedApi.class)
                    .addApiResource(RequestInjectedApi.class)
                    .addApiResource(RequestInjectedSingletonApi.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @BeforeEach
    public void before() {
        SERVICE.reset();
    }

    @Test
    public void fieldInjected() {

        Response r1 = jetty.getTarget().path("f").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("f_1_x_0", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("f").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("f_2_x_0", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void fieldInjectedSingleton() {

        Response r1 = jetty.getTarget().path("fs").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("f_1_x_0", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("fs").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("f_2_x_1", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void constructorInjected() {

        Response r1 = jetty.getTarget().path("c").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("c_1_x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("c").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("c_2_x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void provided() {

        Response r1 = jetty.getTarget().path("u").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("u_1_x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("u").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("u_2_x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void requestInjected() {

        Response r1 = jetty.getTarget().path("r").queryParam("a", "b").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("r_a=b_0", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("r").queryParam("c", "d").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("r_c=d_0", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void requestInjectedSingleton() {

        Response r1 = jetty.getTarget().path("rs").queryParam("a", "b").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("rs_a=b_0", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("rs").queryParam("c", "d").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("rs_c=d_1", r2.readEntity(String.class));
        r2.close();
    }

    @Path("/f")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FieldInjectedApi {

        int count;

        @Inject
        private InjectedService service;

        @Context
        private Configuration config;

        @GET
        public String get() {
            return "f_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY) + "_" + count++;
        }
    }

    @Singleton
    @Path("/fs")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FieldInjectedSingletonApi {

        int count;

        @Inject
        private InjectedService service;

        @Context
        private Configuration config;

        @GET
        public String get() {
            return "f_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY) + "_" + count++;
        }
    }

    @Path("/c")
    @Produces(MediaType.TEXT_PLAIN)
    public static class ConstructorInjectedApi {

        private InjectedService service;

        @Context
        private Configuration config;

        @Inject
        public ConstructorInjectedApi(InjectedService service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "c_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY);
        }
    }

    @Path("/u")
    @Produces(MediaType.TEXT_PLAIN)
    public static class ProviderManagedApi {

        private InjectedService service;

        @Context
        private Configuration config;

        public ProviderManagedApi(InjectedService service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "u_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY);
        }
    }

    @Path("/r")
    @Produces(MediaType.TEXT_PLAIN)
    public static class RequestInjectedApi {

        int count;

        @Context
        private HttpServletRequest request;

        @GET
        public String get() {
            return "r_" + request.getQueryString() + "_" + count++;
        }
    }

    @Singleton
    @Path("/rs")
    @Produces(MediaType.TEXT_PLAIN)
    public static class RequestInjectedSingletonApi {

        int count;

        // This actually works with Jersey, as it injects a proxy into the singleton, swapping it for real request
        // when needed
        @Context
        private HttpServletRequest request;

        @GET
        public String get() {
            return "rs_" + request.getQueryString() + "_" + count++;
        }
    }

    public static class InjectedService {

        private AtomicInteger atomicInt = new AtomicInteger();

        public void reset() {
            atomicInt.set(0);
        }

        public int getNext() {
            return atomicInt.incrementAndGet();
        }
    }
}
