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
package io.bootique.jersey.client.junit5.wiremock;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jersey.client.HttpTargets;
import io.bootique.jersey.client.junit5.wiremock.junit.BaseTest;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestScope;
import io.bootique.junit5.BQTestTool;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class WireMockTester_MultipleIT extends BaseTest {

    protected static final String SERVER_URL2 = "http://localhost:16349";

    @BQApp(BQTestScope.GLOBAL)
    protected static BQRuntime server2 = Bootique.app("--server")
            .autoLoadModules()

            // Unfortunately we have to use the fixed port to be able to record and re-record the service with WireMock
            // So let's use the port that is less likely to cause conflicts with anything
            .module(b -> BQCoreModule.extend(b).setProperty("bq.jetty.connectors[0].port", "16349"))

            .module(b -> JerseyModule.extend(b).addResource(Resource2.class))
            .createRuntime();

    @BQTestTool
    static final WireMockTester tester1 = WireMockTester.create(SERVER_URL);

    @BQTestTool
    static final WireMockTester tester2 = WireMockTester.create(SERVER_URL2);

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(tester1.moduleWithTestTarget("tester1"))
            .module(tester2.moduleWithTestTarget("tester2"))
            .createRuntime();

    @Test
    public void testTwoTargets() {
        WebTarget t1 = app.getInstance(HttpTargets.class).newTarget("tester1");
        JettyTester.assertOk(t1.request().get())
                .assertContentType(MediaType.TEXT_PLAIN)
                .assertContent(c -> assertTrue(c.contains("get")));

        WebTarget t2 = app.getInstance(HttpTargets.class).newTarget("tester2");
        JettyTester.assertOk(t2.request().get())
                .assertContentType(MediaType.TEXT_PLAIN)
                .assertContent(c -> assertTrue(c.contains("get2")));
    }

    @Path("")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource2 {

        @GET
        public String get() {
            return "get2";
        }
    }
}
