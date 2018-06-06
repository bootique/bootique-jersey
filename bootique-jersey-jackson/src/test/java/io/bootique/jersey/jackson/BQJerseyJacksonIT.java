/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.jersey.jackson;

import io.bootique.jersey.JerseyModule;
import io.bootique.test.junit.BQTestFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class BQJerseyJacksonIT {

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory().autoLoadModules();

    private Client client;

    @BeforeClass
    public static void startJetty() {
        TEST_FACTORY.app("-s")
                .autoLoadModules()
                .module(binder -> JerseyModule.extend(binder).addResource(JsonResource.class))
                .run();
    }

    @Before
    public void before() {
        ClientConfig config = new ClientConfig();
        this.client = ClientBuilder.newClient(config);
    }

    @Test
    public void testJacksonSerialization() {
        WebTarget target = client.target("http://127.0.0.1:8080/");

        Response r1 = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("{\"p1\":\"s\",\"p2\":45}", r1.readEntity(String.class));
        r1.close();
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static class JsonResource {

        @GET
        public Model get() {
            return new Model("s", 45);
        }
    }

    public static class Model {
        private String p1;
        private int p2;

        public Model(String p1, int p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        public String getP1() {
            return p1;
        }

        public int getP2() {
            return p2;
        }
    }
}
