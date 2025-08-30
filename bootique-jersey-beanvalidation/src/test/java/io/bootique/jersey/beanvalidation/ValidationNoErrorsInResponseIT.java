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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@BQTest
public class ValidationNoErrorsInResponseIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    private static Consumer<String> assertTrimmed(String expected) {
        return c -> {
            assertNotNull(c);
            assertEquals(expected, c.trim());
        };
    }

    @Test
    public void paramValidation_NotNull() {
        Response ok = jetty.getTarget().path("notNull").queryParam("q", "A").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertOk(ok).assertContent("_A_");

        Response missing = jetty.getTarget().path("notNull").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertBadRequest(missing).assertContent(assertTrimmed(String.format("""
                HTTP ERROR 400 Bad Request
                URI: %s/notNull
                STATUS: 400
                MESSAGE: Bad Request""", jetty.getUrl())));
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
