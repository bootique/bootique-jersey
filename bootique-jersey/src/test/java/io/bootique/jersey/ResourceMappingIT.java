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

import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.test.junit.BQTestFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceMappingIT {

    private static final WebTarget target = ClientBuilder.newClient().target("http://127.0.0.1:8080/");
    @ClassRule
    public static BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @BeforeClass
    public static void startJetty() {
        testFactory.app("-s")
                .module(new TestModule())
                .module(b -> JerseyModule.extend(b)
                        .addResource(new AsInstanceResource("xxx"))
                        .addResource(AsTypeResource.class)
                        .addResource(AsTypePathOverrideResource.class, "as_type_explicit_path")

                        // same resource, two different paths
                        .addResource(AsTypePathOverrideMultipliedResource.class, "as_type_explicit_path1")
                        .addResource(AsTypePathOverrideMultipliedResource.class, "as_type_explicit_path2")

                        .addResource(DIManagedPathOverrideResource.class, "di_managed_explicit_path")
                ).run();
    }

    @Test
    public void testAsInstance() {
        Response r = target.path("as_instance").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("as instance resource - xxx", r.readEntity(String.class));
    }

    @Test
    public void testAsType() {
        Response r = target.path("as_type").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("as type resource", r.readEntity(String.class));
    }

    @Test
    public void testAsType_OverriddenPath() {
        Response r = target.path("as_type_explicit_path").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("as type path override resource: as_type_explicit_path", r.readEntity(String.class));
    }

    @Test
    public void testAsType_MultipleOverriddenPaths() {
        Response r1 = target.path("as_type_explicit_path1").request().get();
        assertEquals(200, r1.getStatus());
        assertEquals("as type path override resource multiplied: as_type_explicit_path1", r1.readEntity(String.class));

        Response r2 = target.path("as_type_explicit_path2").request().get();
        assertEquals(200, r2.getStatus());
        assertEquals("as type path override resource multiplied: as_type_explicit_path2", r2.readEntity(String.class));
    }

    @Test
    public void testDIManaged_OverriddenPath() {
        Response r = target.path("di_managed_explicit_path").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("di managed path override resource: DILABEL", r.readEntity(String.class));
    }

    @Test
    public void testAsType_OverriddenPath_AnnotationPathIgnored() {
        Response r1 = target.path("as_type_annotation_path").request().get();
        assertEquals(404, r1.getStatus());

        Response r2 = target.path("as_type_annotation_path_multiply").request().get();
        assertEquals(404, r2.getStatus());

        Response r3 = target.path("di_managed_annotation_path").request().get();
        assertEquals(404, r3.getStatus());
    }

    @Path("as_instance")
    public static class AsInstanceResource {

        private String label;

        public AsInstanceResource(String label) {
            this.label = label;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get() {
            return Response.ok("as instance resource - " + label).build();
        }
    }

    @Path("as_type")
    public static class AsTypeResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get() {
            return Response.ok("as type resource").build();
        }
    }

    @Path("as_type_annotation_path")
    public static class AsTypePathOverrideResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get(@Context UriInfo uriInfo) {
            return Response.ok("as type path override resource: " + uriInfo.getPath()).build();
        }
    }

    @Path("as_type_annotation_path_multiply")
    public static class AsTypePathOverrideMultipliedResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get(@Context UriInfo uriInfo) {
            return Response.ok("as type path override resource multiplied: " + uriInfo.getPath()).build();
        }
    }

    @Path("di_managed_annotation_path")
    public static class DIManagedPathOverrideResource {

        private String label;

        public DIManagedPathOverrideResource(String label) {
            this.label = label;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get(@Context UriInfo uriInfo) {
            return Response.ok("di managed path override resource: " + label).build();
        }
    }

    public static class TestModule implements BQModule {

        @Override
        public void configure(Binder binder) {

        }

        @Provides
        @Singleton
        DIManagedPathOverrideResource provide() {
            return new DIManagedPathOverrideResource("DILABEL");
        }
    }
}
