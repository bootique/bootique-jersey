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

import io.bootique.BQModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

@Deprecated
@BQTest
public class LegacyResourcePathOverrideIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(TestModule.class)
            .module(b -> JerseyModule.extend(b)
                    .addResource(PathOverrideApi.class, "p1-override")

                    // same resource, two different paths
                    .addResource(PathMultiOverrideApi.class, "p2-override1")
                    .addResource(PathMultiOverrideApi.class, "p2-override2")

                    .addResource(ProvidedSingletonApi.class, "p3-override"))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @Test
    public void pathOverride() {
        JettyTester.assertNotFound(jetty.getTarget().path("p1").request().get());

        Response r = jetty.getTarget().path("p1-override").request().get();
        JettyTester.assertOk(r).assertContent("p1 -> p1-override");
    }

    @Test
    public void pathMultiOverride() {
        JettyTester.assertNotFound(jetty.getTarget().path("p2").request().get());

        Response r1 = jetty.getTarget().path("p2-override1").request().get();
        JettyTester.assertOk(r1).assertContent("p2 -> p2-override1");

        Response r2 = jetty.getTarget().path("p2-override2").request().get();
        JettyTester.assertOk(r2).assertContent("p2 -> p2-override2");
    }

    @Test
    public void providedSingletonOverride() {

        JettyTester.assertNotFound(jetty.getTarget().path("p3").request().get());

        Response r = jetty.getTarget().path("p3-override").request().get();
        JettyTester.assertOk(r).assertContent("p3 -> p3-override");
    }

    @Path("p1")
    public static class PathOverrideApi {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get(@Context UriInfo uriInfo) {
            return Response.ok("p1 -> " + uriInfo.getPath()).build();
        }
    }

    @Path("p2")
    public static class PathMultiOverrideApi {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get(@Context UriInfo uriInfo) {
            return Response.ok("p2 -> " + uriInfo.getPath()).build();
        }
    }

    @Path("p3")
    public static class ProvidedSingletonApi {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get(@Context UriInfo uriInfo) {
            return Response.ok("p3 -> " + uriInfo.getPath()).build();
        }
    }

    public static class TestModule implements BQModule {

        @Override
        public void configure(Binder binder) {
        }

        @Provides
        @Singleton
        ProvidedSingletonApi provide() {
            return new ProvidedSingletonApi();
        }
    }
}
