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
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.di.TypeLiteral;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@BQTest
public class ResourceMappingIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(new TestModule())
            .module(b -> JerseyModule.extend(b)
                    .addResource(new AsInstanceResource("xxx"))
                    .addResource(AsTypeResource.class)
                    .addResource(AsTypePathOverrideResource.class, "as_type_explicit_path")

                    // same resource, two different paths
                    .addResource(AsTypePathOverrideMultipliedResource.class, "as_type_explicit_path1")
                    .addResource(AsTypePathOverrideMultipliedResource.class, "as_type_explicit_path2")

                    .addResource(DIManagedPathOverrideResource.class, "di_managed_explicit_path")

                    .addMappedResource(new TypeLiteral<MappedResource<MappedPathOverrideResource>>() {
                    }))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @Test
    public void testAsInstance() {
        Response r = jetty.getTarget().path("as_instance").request().get();
        JettyTester.assertOk(r).assertContent("as instance resource - xxx");
    }

    @Test
    public void testAsType() {
        Response r = jetty.getTarget().path("as_type").request().get();
        JettyTester.assertOk(r).assertContent("as type resource");
    }

    @Test
    public void testAsType_OverriddenPath() {
        Response r = jetty.getTarget().path("as_type_explicit_path").request().get();
        JettyTester.assertOk(r).assertContent("as type path override resource: as_type_explicit_path");
    }

    @Test
    public void testAsType_MultipleOverriddenPaths() {
        Response r1 = jetty.getTarget().path("as_type_explicit_path1").request().get();
        JettyTester.assertOk(r1).assertContent("as type path override resource multiplied: as_type_explicit_path1");

        Response r2 = jetty.getTarget().path("as_type_explicit_path2").request().get();
        JettyTester.assertOk(r2).assertContent("as type path override resource multiplied: as_type_explicit_path2");
    }

    @Test
    public void testDIManaged_OverriddenPath() {
        Response r = jetty.getTarget().path("di_managed_explicit_path").request().get();
        JettyTester.assertOk(r).assertContent("di managed path override resource: DILABEL");
    }

    @Test
    public void testMapped_MultipleOverriddenPaths() {
        Response r1 = jetty.getTarget().path("mapped_explicit_path1").request().get();
        JettyTester.assertOk(r1).assertContent("mapped path override resource: MAPPEDLABEL");

        Response r2 = jetty.getTarget().path("mapped_explicit_path2").request().get();
        JettyTester.assertOk(r2).assertContent("mapped path override resource: MAPPEDLABEL");
    }

    @Test
    public void testAsType_OverriddenPath_AnnotationPathIgnored() {
        Response r1 = jetty.getTarget().path("as_type_annotation_path").request().get();
        JettyTester.assertNotFound(r1);

        Response r2 = jetty.getTarget().path("as_type_annotation_path_multiply").request().get();
        JettyTester.assertNotFound(r2);

        Response r3 = jetty.getTarget().path("di_managed_annotation_path").request().get();
        JettyTester.assertNotFound(r3);

        Response r4 = jetty.getTarget().path("mapped_annotation_path").request().get();
        JettyTester.assertNotFound(r4);
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

    @Path("mapped_annotation_path")
    public static class MappedPathOverrideResource {

        private String label;

        public MappedPathOverrideResource(String label) {
            this.label = label;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get(@Context UriInfo uriInfo) {
            return Response.ok("mapped path override resource: " + label).build();
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

        @Provides
        @Singleton
        MappedResource<MappedPathOverrideResource> provideMappedResource() {
            return new MappedResource<>(new MappedPathOverrideResource("MAPPEDLABEL"), "mapped_explicit_path1", "mapped_explicit_path2");
        }
    }
}
