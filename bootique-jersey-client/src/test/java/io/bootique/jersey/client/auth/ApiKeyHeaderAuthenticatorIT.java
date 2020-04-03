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
package io.bootique.jersey.client.auth;

import io.bootique.jersey.JerseyModule;
import io.bootique.jersey.client.HttpTargets;
import io.bootique.test.junit.BQTestFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class ApiKeyHeaderAuthenticatorIT {

    @ClassRule
    public static BQTestFactory testFactory = new BQTestFactory();

    @Rule
    public BQTestFactory clientTestFactory = new BQTestFactory();

    @BeforeClass
    public static void beforeClass() {
        testFactory
                .app("-s")
                .autoLoadModules()
                .module((binder) -> JerseyModule.extend(binder).addResource(ProtectedApi.class))
                .run();
    }

    private WebTarget clientTarget(String name) {
        return clientTestFactory.app("-c", "classpath:io/bootique/jersey/client/auth/ApiKeyHeaderAuthenticatorIT.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(HttpTargets.class)
                .newTarget(name);
    }

    @Test
    public void testValidAuth() {

        Response response = clientTarget("valid")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("VALID", response.readEntity(String.class));
    }

    @Test
    public void testInvalidAuth() {

        Response response = clientTarget("invalid")
                .request()
                .get();

        assertEquals(401, response.getStatus());
        assertEquals("INVALID: invalid", response.readEntity(String.class));
    }

    @Test
    public void testCustomHeader() {

        Response response = clientTarget("customValid")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("VALID", response.readEntity(String.class));
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static class ProtectedApi {

        @GET
        @Path("r1")
        public Response r1(@HeaderParam("X-Api-Key") String auth) {
            return "valid".equals(auth)
                    ? Response.ok("VALID").build()
                    : Response.status(Response.Status.UNAUTHORIZED).entity("INVALID: " + auth).build();
        }

        @GET
        @Path("r2")
        public Response r2(@HeaderParam("X-Custom-Api-Key") String auth) {
            return "valid".equals(auth)
                    ? Response.ok("VALID").build()
                    : Response.status(Response.Status.UNAUTHORIZED).entity("INVALID: " + auth).build();
        }
    }
}
