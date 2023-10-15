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
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class JerseyClientModuleIT {
    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("--server")
            .autoLoadModules()
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @BQTestTool
    final BQTestFactory clientFactory = new BQTestFactory();

    @Test
    public void testGet() {
        BQRuntime clientApp = clientFactory.app()
                .autoLoadModules()
                .createRuntime();

        Client client = clientApp.getInstance(HttpClientFactory.class).newClient();
        Response r1 = client.target(jetty.getUrl()).path("get").request().get();
        JettyTester.assertOk(r1).assertContent("got");
    }

    @Test
    public void testGetRx() throws ExecutionException, InterruptedException {
        BQRuntime clientApp = clientFactory.app()
                .autoLoadModules()
                .createRuntime();

        Client client = clientApp.getInstance(HttpClientFactory.class).newClient();
        Response r1 = client.target(jetty.getUrl()).path("get").request().rx().get().toCompletableFuture().get();
        JettyTester.assertOk(r1).assertContent("got");
    }

    @Test
    public void testGetRxThenApply() throws ExecutionException, InterruptedException {
        BQRuntime clientApp = clientFactory.app()
                .autoLoadModules()
                .createRuntime();

        Client client = clientApp.getInstance(HttpClientFactory.class).newClient();
        WebTarget target = client.target(jetty.getUrl()).path("get");
        Response r1 = target.request().rx()
                .get()
                .thenApply(r -> target.queryParam("q", r.readEntity(String.class)).request().get())
                .toCompletableFuture()
                .get();

        JettyTester.assertOk(r1).assertContent("got?q=got");
    }

    @Test
    public void testGetRx_ExecutorDefaultParams() {
        BQRuntime clientApp = clientFactory.app()
                .autoLoadModules()
                .createRuntime();

        Client client = clientApp.getInstance(HttpClientFactory.class).newClient();
        ExecutorServiceCapture esc = new ExecutorServiceCapture();
        client.target(jetty.getUrl()).path("get").register(esc).request().rx().get().toCompletableFuture().join();

        assertNotNull(esc.executorService);
        assertTrue(esc.executorService instanceof ThreadPoolExecutor);

        ThreadPoolExecutor tpe = (ThreadPoolExecutor) esc.executorService;
        assertEquals(0, tpe.getCorePoolSize());
        assertEquals(Integer.MAX_VALUE, tpe.getMaximumPoolSize());
        assertEquals(60, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }

    @Test
    public void testGetRx_ExecutorCustomParams() {
        BQRuntime clientApp = clientFactory.app()
                .autoLoadModules()
                .property("bq.jerseyclient.asyncThreadPoolSize", "20")
                .createRuntime();

        Client client = clientApp.getInstance(HttpClientFactory.class).newClient();
        ExecutorServiceCapture esc = new ExecutorServiceCapture();
        client.target(jetty.getUrl()).path("get").register(esc).request().rx().get().toCompletableFuture().join();

        assertNotNull(esc.executorService);
        assertTrue(esc.executorService instanceof ThreadPoolExecutor);

        ThreadPoolExecutor tpe = (ThreadPoolExecutor) esc.executorService;
        assertEquals(20, tpe.getCorePoolSize());
        assertEquals(20, tpe.getMaximumPoolSize());
        assertEquals(60, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }

    @Test
    public void testGet_Gzip() {
        BQRuntime clientApp = clientFactory.app()
                .autoLoadModules()
                .createRuntime();

        Client client = clientApp.getInstance(HttpClientFactory.class).newClient();

        // gzip encoding on the server kicks in after a certain size. So request a bigger chunk of content
        Response r1 = client.target(jetty.getUrl()).path("get-large").request().get();
        JettyTester.assertOk(r1)
                .assertHeader("Content-Encoding", "gzip")
                .assertContent(c -> Assertions.assertTrue(c.startsWith("got-large")));
    }


    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        @Path("get")
        public String get(@QueryParam("q") String q) {
            String suffix = q != null ? "?q=" + q : "";
            return "got" + suffix;
        }

        @GET
        @Path("get-large")
        public String getLarge() {
            return "got-large:" + "aaaaaaaaaaaaaaaaaaaaaaaaaa ".repeat(1000);
        }
    }

    static class ExecutorServiceCapture implements ClientRequestFilter {

        ExecutorService executorService;

        @Override
        public void filter(ClientRequestContext c) throws IOException {
            this.executorService = ((ClientConfig) c.getConfiguration()).getExecutorService();
        }
    }
}
