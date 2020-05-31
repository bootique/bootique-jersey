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
import io.bootique.di.Injector;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.ws.rs.*;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class OAuth2AuthenticatorFactoryIT {

    @BQApp
    static final BQRuntime server = Bootique.app("-s")
            .autoLoadModules()
            .module((binder) -> JerseyModule.extend(binder).addResource(TokenApi.class))
            .module(JettyTester.moduleReplacingConnectors())
            .createRuntime();

    @RegisterExtension
    public BQTestFactory clientFactory = new BQTestFactory();

    private Injector clientStackInjector() {
        return clientFactory.app()
                .autoLoadModules()
                .createRuntime()
                .getInstance(Injector.class);
    }

    @Test
    public void testGetToken() {

        OAuth2AuthenticatorFactory factory = new OAuth2AuthenticatorFactory();
        factory.setPassword("p");
        factory.setUsername("u");
        factory.setTokenUrl(JettyTester.getServerUrl(server) + "/token");

        OAuth2Token token = factory
                .createOAuth2TokenDAO(clientStackInjector())
                .getToken();

        assertNotNull(token);
        assertEquals("t:client_credentials:Basic dTpw", token.getAccessToken());
    }

    @Test
    public void testGetToken_Error() {

        OAuth2AuthenticatorFactory factory = new OAuth2AuthenticatorFactory();
        factory.setPassword("p");
        factory.setUsername("u");
        factory.setTokenUrl(JettyTester.getServerUrl(server) + "/token_error");

        assertThrows(RuntimeException.class, () -> factory.createOAuth2TokenDAO(clientStackInjector()).getToken());
    }

    @Test
    public void testGetWithToken() {

        OAuth2AuthenticatorFactory factory = new OAuth2AuthenticatorFactory();
        factory.setPassword("p");
        factory.setUsername("u");
        factory.setTokenUrl(JettyTester.getServerUrl(server) + "/token");

        ClientRequestFilter filter = factory.createAuthFilter(clientStackInjector());

        Response r1 = ClientBuilder
                .newClient()
                .register(filter)
                .target(JettyTester.getServerUrl(server) + "/require_token")
                .request()
                .get();

        assertEquals(200, r1.getStatus());

        Response r2 = ClientBuilder
                .newClient()
                .register(filter)
                .target(JettyTester.getServerUrl(server) + "/require_token")
                .request()
                .get();

        assertEquals(200, r2.getStatus());
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static class TokenApi {

        @POST
        @Path("token")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String post(@FormParam("grant_type") String grantType, @HeaderParam("authorization") String auth) {
            return String.format(
                    "{\"access_token\":\"t:%s:%s\",\"token_type\":\"example\","
                            + "\"expires_in\":3600,\"refresh_token\":\"bla\",\"example_parameter\":\"example_value\"}",
                    grantType, auth);
        }

        @POST
        @Path("token_error")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public Response post_error(@FormParam("grant_type") String grantType,
                                   @HeaderParam("authorization") String auth) {
            return Response.status(Status.BAD_REQUEST).entity("{\"error\":\"invalid_request\"}").build();
        }

        @GET
        @Path("require_token")
        public Response getWithToken(@HeaderParam("authorization") String auth) {
            return auth != null && auth.toLowerCase().startsWith("bearer ")
                    ? Response.ok().build()
                    : Response.status(Status.BAD_REQUEST).build();
        }
    }
}
