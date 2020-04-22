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
package io.bootique.jersey.beanvalidation;

import io.bootique.jersey.JerseyModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class ValidationNoErrorsInResponseIT {

    @ClassRule
    public static BQTestFactory testFactory = new BQTestFactory();

    private static WebTarget baseTarget = ClientBuilder.newClient().target("http://127.0.0.1:8080/");

    @BeforeClass
    public static void beforeAll() {
        testFactory.app("-s")
                .autoLoadModules()
                .module(b -> JerseyModule.extend(b).addResource(Resource.class))
                .run();
    }

    @Test
    public void testParamValidation_NotNull() {
        Response ok = baseTarget.path("notNull").queryParam("q", "A").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, ok.getStatus());
        assertEquals("_A_", ok.readEntity(String.class));

        Response missing = baseTarget.path("notNull").request(MediaType.TEXT_PLAIN).get();
        assertEquals(400, missing.getStatus());
        assertEquals("HTTP ERROR 400 Bad Request\n" +
                "URI: /notNull\n" +
                "STATUS: 400\n" +
                "MESSAGE: Bad Request\n" +
                "SERVLET: jersey", missing.readEntity(String.class).trim());
    }

    @Path("/")
    public static class Resource {

        @GET
        @Path("notNull")
        @Produces(MediaType.TEXT_PLAIN)
        public Response getNotNull(@NotNull(message = "'q' is required") @QueryParam("q") String q) {
            return Response.ok("_" + q + "_", MediaType.TEXT_PLAIN_TYPE).build();
        }
    }
}
