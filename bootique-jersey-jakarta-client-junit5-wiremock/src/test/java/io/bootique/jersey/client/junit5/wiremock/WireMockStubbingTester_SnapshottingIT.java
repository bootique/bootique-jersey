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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.client.HttpTargets;
import io.bootique.jersey.client.junit5.wiremock.junit.TestWithEmulatedBackend;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestTool;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class WireMockStubbingTester_SnapshottingIT extends TestWithEmulatedBackend {

    @BQTestTool
    static final WireMockStubbingTester tester = WireMockTester
            .stubbingTester()
            .stub(proxyAllTo(SERVER_URL).atPriority(10));

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(tester.moduleWithTestTarget("tester"))
            .createRuntime();

    @Test
    public void test() {
        WebTarget target = app.getInstance(HttpTargets.class).newTarget("tester").path("p1");
        JettyTester.assertOk(target.request().get())
                .assertContentType(MediaType.TEXT_PLAIN)
                .assertContent(c -> assertTrue(c.contains("get:p1")));
    }
}
