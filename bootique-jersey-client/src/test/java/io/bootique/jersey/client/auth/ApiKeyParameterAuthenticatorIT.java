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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jersey.client.HttpTargets;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@BQTest
public class ApiKeyParameterAuthenticatorIT {

    // TODO: using port 8080 on the server as it is configured for the client targets

    @BQApp
    static final BQRuntime server = Bootique.app("-s")
            .autoLoadModules()
            .module((binder) -> JerseyModule.extend(binder).addResource(ProtectedApi.class))
            .createRuntime();

    @BQTestTool
    final BQTestFactory clientTestFactory = new BQTestFactory();

    private WebTarget startClient(String targetName) {
        return clientTestFactory.app("-c", "classpath:io/bootique/jersey/client/auth/ApiKeyParameterAuthenticatorIT.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(HttpTargets.class)
                .newTarget(targetName);
    }

    @Test
    public void testValidAuth() {
        Response response = startClient("valid")
                .request()
                .get();

        JettyTester.assertOk(response).assertContent("VALID");
    }

    @Test
    public void testValidAuthWithParams() {
        Response response = startClient("valid")
                .queryParam("x", "y")
                .request()
                .get();

        JettyTester.assertOk(response).assertContent("VALID;x=y");
    }

    @Test
    public void testInvalidAuth() {
        Response response = startClient("invalid")
                .request()
                .get();

        JettyTester.assertStatus(response, 401).assertContent("INVALID: invalid");
    }

    @Test
    public void testCustomParameter() {

        Response response = startClient("customValid")
                .request()
                .get();

        JettyTester.assertOk(response).assertContent("VALID");
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static class ProtectedApi {

        @GET
        @Path("r1")
        public Response r1(@QueryParam("api_key") String auth, @QueryParam("x") String x) {

            String suffix = x != null ? ";x=" + x : "";

            return "valid".equals(auth)
                    ? Response.ok("VALID" + suffix).build()
                    : Response.status(Response.Status.UNAUTHORIZED).entity("INVALID: " + auth + suffix).build();
        }

        @GET
        @Path("r2")
        public Response r2(@QueryParam("custom_api_key") String auth) {
            return "valid".equals(auth)
                    ? Response.ok("VALID").build()
                    : Response.status(Response.Status.UNAUTHORIZED).entity("INVALID: " + auth).build();
        }
    }
}
