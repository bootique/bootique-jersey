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

package io.bootique.jersey.jakarta;

import io.bootique.jetty.jakarta.junit5.JettyTester;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

@BQTest
public class ApplicationIT {


    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    @DisplayName("@ApplicationPath annotation must set JAX-RS base path")
    public void testPathFromAnnotation() {

        JettyTester jetty = JettyTester.create();
        testFactory.app("-s")
                .module(jetty.moduleReplacingConnectors())
                .module(b -> JerseyModule.extend(b).setApplication(App1.class))
                .module(b -> JerseyModule.extend(b).addResource(Resource1.class))
                .run();

        Response r = jetty.getTarget().path("app1path/r1").request().get();
        JettyTester.assertOk(r).assertContent("r1_called");
    }


    @Test
    @DisplayName("Application-provided endpoints must be deployed")
    public void testAppResources() {

        JettyTester jetty = JettyTester.create();
        testFactory.app("-s")
                .module(jetty.moduleReplacingConnectors())
                .module(b -> b.bind(Echo.class).toInstance(new Echo("echo")))
                .module(b -> JerseyModule.extend(b).setApplication(App2.class))
                .run();

        Response r = jetty.getTarget().path("r2").request().get();

        // testing both resource registration and BQ injection into resource
        JettyTester.assertOk(r).assertContent("echo: r2_called");
    }

    @ApplicationPath("app1path")
    public static class App1 extends Application {
    }

    public static class App2 extends Application {

        @Override
        public Set<Class<?>> getClasses() {
            return Collections.singleton(Resource2.class);
        }
    }

    @Path("r1")
    public static class Resource1 {

        @GET
        public Response get() {
            return Response.ok("r1_called", MediaType.TEXT_PLAIN_TYPE).build();
        }
    }

    @Path("r2")
    public static class Resource2 {

        @Inject
        private Echo echo;

        @GET
        public Response get() {
            return Response.ok(echo.get("r2_called"), MediaType.TEXT_PLAIN_TYPE).build();
        }
    }

    public static class Echo {

        private String prefix;

        public Echo(String prefix) {
            this.prefix = prefix;
        }

        public String get(String in) {
            return prefix + ": " + in;
        }
    }
}
