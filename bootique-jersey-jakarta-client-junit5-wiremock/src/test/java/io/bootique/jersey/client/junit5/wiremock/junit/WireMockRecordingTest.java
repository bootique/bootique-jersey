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
package io.bootique.jersey.client.junit5.wiremock.junit;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestScope;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@BQTest
public abstract class WireMockRecordingTest {

    protected static final String SERVER_URL = "http://localhost:16348";

    @BQApp(BQTestScope.GLOBAL)
    protected static BQRuntime server = Bootique.app("--server")
            .autoLoadModules()

            // Unfortunately we have to use the fixed port to be able to record and re-record the service with WireMock
            // So let's use the port that is less likely to cause conflicts with anything
            .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.connectors[0].port", "16348"))

            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .createRuntime();

    @Path("")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        public Response get(@QueryParam("q") String q) {
            String response = q != null ? "get:" + q : "get";
            return Response.ok().entity(response).build();
        }

        @GET
        @Path("/p1")
        public Response getPath() {
            return Response.ok().entity("get:p1").build();
        }

        @GET
        @Path("/p1/p11")
        public Response getSubPath() {
            return Response.ok().entity("get:p1:p11").build();
        }
    }
}
