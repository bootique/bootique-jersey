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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Demonstrates several issues with @Inject and @Named annotations")
@BQTest
public class IssuesNamedAnnotationInjectionIT {

    private static final String TEST_PROPERTY = "bq.test.label";
    private static final InjectedServiceInterface serviceA = new InjectedServiceImplA();
    private static final InjectedServiceInterface serviceB = new InjectedServiceImplB();
    private static final InjectedServiceInterface serviceC = new InjectedServiceImplC();

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> b.bind(InjectedServiceInterface.class, "A").toInstance(serviceA))
            .module(b -> b.bind(InjectedServiceInterface.class, "B").toInstance(serviceB))
            .module(b -> b.bind(InjectedServiceInterface.class, CustomQualifierC.class).toInstance(serviceC))
            .module(b -> JerseyModule.extend(b)
                    .addFeature(ctx -> {
                        ctx.property(TEST_PROPERTY, "x");
                        return false;
                    })
                    .addResource(NamedFieldInjectedResourceJavaXAnnotations.class)
                    .addResource(NamedFieldInjectedResourceBQAnnotations.class)
                    .addResource(NamedFieldInjectedResourceCustomJavaXAnnotations.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @BeforeEach
    public void before() {
        serviceA.reset();
        serviceB.reset();
        serviceC.reset();
    }


    @Test
    public void testNamedFieldInjectedJakartaAnnotations() {

        Response r1 = jetty.getTarget().path("nfJakartaInject").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("nf_1x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("nfJakartaInject").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("nf_2x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testNamedFieldInjectedBQAnnotations() {

        Response r1 = jetty.getTarget().path("nfBQInject").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("nf_1x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("nfBQInject").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("nf_2x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testNamedFieldInjectedCustomJavaXAnnotations() {

        Response r1 = jetty.getTarget().path("nfCustomInjectJavaX").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("nfC_1x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("nfCustomInjectJavaX").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("nfC_2x", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testNamedFieldInjectedCustomJakartaAnnotations() {

        Response r1 = jetty.getTarget().path("nfCustomInjectJakarta").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("nfD_1x", r1.readEntity(String.class));
        r1.close();

        Response r2 = jetty.getTarget().path("nfCustomInjectJakarta").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("nfD_2x", r2.readEntity(String.class));
        r2.close();
    }


    @Path("/nfJavaXInject")
    @Produces(MediaType.TEXT_PLAIN)
    public static class NamedFieldInjectedResourceJavaXAnnotations {

        @Inject
        @Named("A")
        private InjectedServiceInterface serviceA;

        @Context
        private Configuration config;

        @GET
        public String get() {
            return "nf_" + serviceA.getNext() + config.getProperty(TEST_PROPERTY);
        }
    }

    @Path("/nfBQInject")
    @Produces(MediaType.TEXT_PLAIN)
    public static class NamedFieldInjectedResourceBQAnnotations {

        @BQInject
        @Named("B")
        private InjectedServiceInterface serviceB;

        @Context
        private Configuration config;

        @GET
        public String get() {
            return "nf_" + serviceB.getNext() + config.getProperty(TEST_PROPERTY);
        }
    }

    @Path("/nfCustomInjectJavaX")
    @Produces(MediaType.TEXT_PLAIN)
    public static class NamedFieldInjectedResourceCustomJavaXAnnotations {

        @Inject
        @CustomQualifierC
        private InjectedServiceInterface serviceC;

        @Context
        private Configuration config;

        @GET
        public String get() {
            return "nfC_" + serviceC.getNext() + config.getProperty(TEST_PROPERTY);
        }
    }


    public static interface InjectedServiceInterface {
        AtomicInteger atomicInt = new AtomicInteger();
        default void reset() {
            atomicInt.set(0);
        }
        int getNext();
    }

    public static class InjectedServiceImplA implements InjectedServiceInterface {
        public int getNext() {
            return atomicInt.incrementAndGet();
        }
    }

    public static class InjectedServiceImplB implements InjectedServiceInterface {
        public int getNext() {
            return atomicInt.incrementAndGet();
        }
    }

    public static class InjectedServiceImplC implements InjectedServiceInterface {
        public int getNext() {
            return atomicInt.incrementAndGet();
        }
    }

    @java.lang.annotation.Documented
    @java.lang.annotation.Retention(RUNTIME)
    @javax.inject.Qualifier
    public @interface CustomQualifierC {
    }

}
