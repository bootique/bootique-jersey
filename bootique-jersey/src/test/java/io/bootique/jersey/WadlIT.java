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

import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

@BQTest
public class WadlIT {

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    // can't test WADL enablement per https://github.com/bootique/bootique-jersey/issues/100 , as this would require
    // expanding test classpath with a JAXB impl. We don't JAXB to be there for tests. TODO: a separate test module?

    @Test
    public void wadlOff() {

        JettyTester jetty = JettyTester.create();
        testFactory.app("-s")
                .module(jetty.moduleReplacingConnectors())
                .module(b -> JerseyModule.extend(b).addResource(Resource1.class))
                .run();

        Response r = jetty.getTarget().path("application.wadl").request().get();
        JettyTester.assertNotFound(r);
    }


    @Test
    public void wadlOnButNoJAXB() {

        // in addition to enabling via the prperty, WADL checks for JAXB presence, and we don't have it by default

        JettyTester jetty = JettyTester.create();
        testFactory.app("-s")
                .module(jetty.moduleReplacingConnectors())
                .module(b -> JerseyModule.extend(b).addResource(Resource1.class).setProperty(JerseyModule.DISABLE_WADL_PROPERTY, false))
                .run();

        Response r = jetty.getTarget().path("application.wadl").request().get();
        JettyTester.assertNotFound(r);
    }

    @Path("r1")
    public static class Resource1 {

        @GET
        public Response get() {
            return Response.ok("r1_called", MediaType.TEXT_PLAIN_TYPE).build();
        }
    }
}
