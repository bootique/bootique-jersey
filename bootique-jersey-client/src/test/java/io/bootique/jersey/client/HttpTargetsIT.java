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

package io.bootique.jersey.client;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.*;
import io.bootique.logback.LogbackModule;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@BQTest
public class HttpTargetsIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime server = Bootique.app("--server")
            .modules(JettyModule.class, JerseyModule.class, LogbackModule.class)
            .module(b -> JerseyModule.extend(b).addApiResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @BQTestTool
    public BQTestFactory clientFactory = new BQTestFactory();

    @Test
    public void newTarget() {
        HttpTargets targets = clientFactory.app()
                .modules(JerseyClientModule.class, LogbackModule.class)
                .property("bq.jerseyclient.targets.t1.url", JettyTester.getUrl(server) + "/get")
                .createRuntime()
                .getInstance(HttpTargets.class);

        WebTarget t1 = targets.newTarget("t1");

        Response r1 = t1.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus()) ;
        assertEquals("got", r1.readEntity(String.class));

        Response r2 = t1.path("me").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("got/me", r2.readEntity(String.class));
    }

    @Test
    public void newTarget_Auth() {
        HttpTargets targets =
                clientFactory.app()
                        .modules(JerseyClientModule.class, LogbackModule.class)
                        .property("bq.jerseyclient.auth.a1.type", "basic")
                        .property("bq.jerseyclient.auth.a1.username", "u")
                        .property("bq.jerseyclient.auth.a1.password", "p")
                        .property("bq.jerseyclient.targets.t1.url", JettyTester.getUrl(server) + "/get_auth")
                        .property("bq.jerseyclient.targets.t1.auth", "a1")
                        .createRuntime()
                        .getInstance(HttpTargets.class);

        WebTarget t1 = targets.newTarget("t1");

        Response r1 = t1.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("got_Basic dTpw", r1.readEntity(String.class));
    }

    @Test
    public void newTarget_Redirects_Default_Follow() {
        new FollowRedirectsTester(null, null).expectFollow();
    }

    @Test
    public void newTarget_Redirects_ClientNoFollow_TargetDefault() {
        new FollowRedirectsTester(false, null).expectNoFollow();
    }

    @Test
    public void newTarget_Redirects_ClientNoFollow_TargetFollow() {
        new FollowRedirectsTester(false, true).expectFollow();
    }

    @Test
    public void newTarget_Compression_Default_Compressed() {
        new CompressionTester(null, null).expectCompressed();
    }

    @Test
    public void newTarget_Compression_ClientNotCompressed_TargetDefault() {
        new CompressionTester(false, null).expectUncompressed();
    }

    @Test
    public void newTarget_Compression_ClientNotCompressed_TargetCompressed() {
        new CompressionTester(false, true).expectCompressed();
    }

    @Test
    public void newTarget_Compression_ClientCompressed_TargetCompressed() {
        new CompressionTester(true, true).expectCompressed();
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        @Path("get")
        public String get() {
            return "got";
        }

        @GET
        @Path("get/me")
        public String getMe() {
            return "got/me";
        }

        @GET
        @Path("get_auth")
        public String getAuth(@HeaderParam("Authorization") String auth) {
            return "got_" + auth;
        }

        @GET
        @Path("302")
        public Response threeOhTwo() throws URISyntaxException {
            return Response.temporaryRedirect(new URI("/get")).build();
        }


        @GET
        @Path("getbig")
        // value big enough to ensure compression kicks in
        public String getBig() {
            return "gotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgot";
        }
    }

    class FollowRedirectsTester {
        private Boolean clientRedirects;
        private Boolean targetRedirects;

        public FollowRedirectsTester(Boolean clientRedirects, Boolean targetRedirects) {
            this.clientRedirects = clientRedirects;
            this.targetRedirects = targetRedirects;
        }

        public void expectFollow() {
            Response r = createTarget().request().get();
            assertEquals(200, r.getStatus());
            assertEquals("got", r.readEntity(String.class));
        }

        public void expectNoFollow() {
            Response r = createTarget().request().get();
            assertEquals(307, r.getStatus());
            assertEquals(JettyTester.getUrl(server) + "/get", r.getHeaderString("location"));
        }

        private WebTarget createTarget() {

            TestRuntumeBuilder builder = clientFactory.app()
                    .modules(JerseyClientModule.class, LogbackModule.class)
                    .property("bq.jerseyclient.targets.t.url", JettyTester.getUrl(server) + "/302");

            if (clientRedirects != null) {
                builder.property("bq.jerseyclient.followRedirects", clientRedirects.toString());
            }

            if (targetRedirects != null) {
                builder.property("bq.jerseyclient.targets.t.followRedirects", targetRedirects.toString());
            }

            return builder.createRuntime().getInstance(HttpTargets.class).newTarget("t");
        }
    }

    class CompressionTester {
        private Boolean clientCompression;
        private Boolean targetCompression;

        public CompressionTester(Boolean clientCompression, Boolean targetCompression) {
            this.clientCompression = clientCompression;
            this.targetCompression = targetCompression;
        }

        public void expectCompressed() {
            Response r = createTarget().request().get();
            assertEquals(200, r.getStatus());
            assertEquals("gzip", r.getHeaderString("Content-Encoding"));
        }

        public void expectUncompressed() {
            Response r = createTarget().request().get();
            assertEquals(200, r.getStatus());
            assertNull(r.getHeaderString("Content-Encoding"));
        }

        private WebTarget createTarget() {

            TestRuntumeBuilder builder = clientFactory.app()
                    .modules(JerseyClientModule.class, LogbackModule.class)
                    .property("bq.jerseyclient.targets.t.url", JettyTester.getUrl(server) + "/getbig");

            if (clientCompression != null) {
                builder.property("bq.jerseyclient.compression", clientCompression.toString());
            }

            if (targetCompression != null) {
                builder.property("bq.jerseyclient.targets.t.compression", targetCompression.toString());
            }

            return builder.createRuntime().getInstance(HttpTargets.class).newTarget("t");
        }
    }
}
