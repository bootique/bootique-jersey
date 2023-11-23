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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@BQTest
public class BQJerseyJackson_JsonSerializerIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> JerseyModule.extend(b).addResource(JsonResource.class))
            .module(b -> JerseyJacksonModule.extend(b).addSerializer(XSerializer.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @Test
    public void jacksonSerialization() {
        Response r = jetty.getTarget().request().get();
        JettyTester.assertOk(r).assertContent("{\"p1\":\"s\",\"x\":\"xxxxx\"}");
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static class JsonResource {

        @GET
        public Model get() {
            return new Model("s", new X("xxxxx"));
        }

        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        public Response echo(Model model) {
            return Response.ok().entity(model).build();
        }
    }

    public static class X {
        private String string;

        public X(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }
    }

    public static class Model {
        private String p1;
        private X x;

        public Model(String p1, X x) {
            this.p1 = p1;
            this.x = x;
        }

        public String getP1() {
            return p1;
        }

        public X getX() {
            return x;
        }
    }

    public static class XSerializer extends JsonSerializer<X> {

        @Override
        public void serialize(X value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getString());
        }

        @Override
        public Class<X> handledType() {
            return X.class;
        }
    }
}
