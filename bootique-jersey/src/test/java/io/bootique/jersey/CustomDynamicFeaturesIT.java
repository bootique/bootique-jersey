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

import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class CustomDynamicFeaturesIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testFeaturesLoaded() {

        testFactory.app("-s")
                .module(b -> JerseyModule
                        .extend(b)
                        .addDynamicFeature(DynamicFeature1.class)
                        .addDynamicFeature(DynamicFeature2.class)

                        // since 2.35 need at least one resource to exist in the container,
                        // or dynamic features won't be loaded
                        .addResource(Resource.class))
                .run();

        assertTrue(DynamicFeature1.LOADED);
        assertTrue(DynamicFeature2.LOADED);
    }

    static class DynamicFeature1 implements DynamicFeature {

        static boolean LOADED = false;

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            LOADED = true;
        }
    }

    static class DynamicFeature2 implements DynamicFeature {

        static boolean LOADED = false;

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            LOADED = true;
        }
    }

    @Path("/")
    public static class Resource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response get(@Context UriInfo uriInfo) {
            return Response.ok("Hi").build();
        }
    }
}
