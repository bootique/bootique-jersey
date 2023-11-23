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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@BQTest
public class ExceptionMapperIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> JerseyModule.extend(b)
                    .addResource(Resource.class)
                    .addResource(MyExceptionMapper.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @Test
    public void customExceptionMapper() {
        Response r = jetty.getTarget().path("my-exception").queryParam("q", "p").request().get();
        JettyTester.assertBadRequest(r).assertContent("E:p");
    }

    @Test
    public void defaultExceptionMapper() {
        Response r = jetty.getTarget().path("web-exception").queryParam("q", "p").request().get();
        JettyTester.assertStatus(r, 500);
    }

    public static final class MyExceptionMapper implements ExceptionMapper<MyException> {

        @Override
        public Response toResponse(MyException exception) {
            return Response
                    .status(400)
                    .entity("E:" + exception.getMessage())
                    .build();
        }
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        @Path("my-exception")
        public String my(@QueryParam("q") String q) {
            throw new MyException(q);
        }

        @GET
        @Path("web-exception")
        public String web(@QueryParam("q") String q) {
            throw new WebApplicationException(q);
        }
    }

    public static class MyException extends RuntimeException {
        public MyException(String message) {
            super(message);
        }
    }
}
