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

package io.bootique.jersey.jackson;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

@BQTest
public class BQJerseyJacksonIT {

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(binder -> JerseyModule.extend(binder).addResource(JsonResource.class))
            .module(JettyTester.moduleReplacingConnectors())
            .createRuntime();

    @Test
    public void testJacksonSerialization() {
        Response r = JettyTester.getTarget(app).request().get();
        JettyTester.assertOk(r).assertContent("{\"p1\":\"s\",\"p2\":45,\"ts\":\"2020-01-02T03:04:05\"}");
    }

    @Test
    public void testJacksonDeserialization() {
        String entity = "{\"p1\":\"xx\",\"p2\":55,\"ts\":\"2021-01-02T01:04:05\"}";
        Response r = JettyTester.getTarget(app).request().put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
        JettyTester.assertOk(r).assertContent(entity);
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static class JsonResource {

        @GET
        public Model get() {
            return new Model("s", 45, LocalDateTime.of(2020, 1, 2, 3, 4, 5));
        }

        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        public Response echo(Model model) {
            return Response.ok().entity(model).build();
        }
    }

    public static class Model {
        private String p1;
        private int p2;
        private LocalDateTime ts;

        public Model() {
        }

        public Model(String p1, int p2, LocalDateTime ts) {
            this.p1 = p1;
            this.p2 = p2;
            this.ts = ts;
        }

        public String getP1() {
            return p1;
        }

        public int getP2() {
            return p2;
        }

        public LocalDateTime getTs() {
            return ts;
        }
    }
}
