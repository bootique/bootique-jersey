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
import io.bootique.di.BQInject;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class ResourceInjectionIT {

    private static final String TEST_PROPERTY = "bq.test.label";
    private static final InjectedService service = new InjectedService();
    private static final InjectedServiceInterface serviceA = new InjectedServiceImplA();
    private static final InjectedServiceInterface serviceB = new InjectedServiceImplB();

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> b.bind(InjectedService.class).toInstance(service))
            .module(b -> b.bind(InjectedServiceInterface.class, "A").toInstance(serviceA))
            .module(b -> b.bind(InjectedServiceInterface.class, "B").toInstance(serviceB))
            .module(b -> b.bind(UnInjectedResource.class).toProviderInstance(() -> new UnInjectedResource(service)))
            .module(b -> JerseyModule.extend(b)
                    .addFeature(ctx -> {
                        ctx.property(TEST_PROPERTY, "x");
                        return false;
                    })
                    .addResource(FieldInjectedResource.class)
                    .addResource(NamedFieldInjectedResource.class)
                    .addResource(ConstructorInjectedResource.class)
                    .addResource(UnInjectedResource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @BeforeEach
    public void before() {
        service.reset();
    }

    @Test
    public void testFieldInjected() {

        Response r1 = jetty.getTarget().path("f").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("f_1_x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("f").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("f_2_x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testNamedFieldInjected() {

        Response r1 = jetty.getTarget().path("nf").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("nf_1_2_x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("nf").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("nf_3_4_x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testConstructorInjected() {

        Response r1 = jetty.getTarget().path("c").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("c_1_x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("c").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("c_2_x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testProviderForResource() {

        Response r1 = jetty.getTarget().path("u").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("u_1_x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("u").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("u_2_x", r2.readEntity(String.class));
        r2.close();
    }

    @Path("/f")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FieldInjectedResource {

        @Inject
        private InjectedService service;

        @Context
        private Configuration config;

        @GET
        public String get() {
            return "f_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY);
        }
    }

    @Path("/nf")
    @Produces(MediaType.TEXT_PLAIN)
    public static class NamedFieldInjectedResource {

        @Inject
        @Named("A")
        private InjectedServiceInterface serviceA;

        @Inject
        @Named("B")
        private InjectedServiceInterface serviceB;

        @Context
        private Configuration config;

        @GET
        public String get() {
            return "nf_" + serviceA.getNext() + "_" + serviceA.getNext()+ "_" + config.getProperty(TEST_PROPERTY);
        }
    }

    @Path("/c")
    @Produces(MediaType.TEXT_PLAIN)
    public static class ConstructorInjectedResource {

        private InjectedService service;

        @Context
        private Configuration config;

        @Inject
        public ConstructorInjectedResource(InjectedService service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "c_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY);
        }
    }

    @Path("/u")
    @Produces(MediaType.TEXT_PLAIN)
    public static class UnInjectedResource {

        private InjectedService service;

        @Context
        private Configuration config;

        public UnInjectedResource(InjectedService service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "u_" + service.getNext() + "_" + config.getProperty(TEST_PROPERTY);
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

    public static interface InjectedServiceInterface {
        public void reset();
        public int getNext();
    }

    public static class InjectedServiceImplA implements InjectedServiceInterface {

        private AtomicInteger atomicInt = new AtomicInteger();

        public void reset() {
            atomicInt.set(0);
        }

        public int getNext() {
            return atomicInt.incrementAndGet();
        }
    }

    public static class InjectedServiceImplB implements InjectedServiceInterface {

        private AtomicInteger atomicInt = new AtomicInteger();

        public void reset() {
            atomicInt.set(0);
        }

        public int getNext() {
            return atomicInt.incrementAndGet();
        }
    }
}
