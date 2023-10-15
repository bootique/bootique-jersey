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

package io.bootique.jersey.client.instrumented;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jersey.client.HttpClientFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.metrics.mdc.TransactionIdMDC;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@BQTest
public class InstrumentedClientMDCIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime serverApp = Bootique.app("--server")
            .modules(JettyModule.class, JerseyModule.class)
            .module(binder -> JerseyModule.extend(binder).addResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @BQApp(skipRun = true)
    final BQRuntime clientApp = Bootique.app()
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b).setProperty("bq.log.logFormat", "[%date{\"dd/MMM/yyyy:HH:mm:ss,SSS\"}] %thread %level %mdc{txid:-?} %logger{1}: %msg%n%ex"))
            .createRuntime();

    @AfterEach
    void clearMDC() {
        TransactionIdMDC.clearId();
    }

    @Test
    public void testNoMDCRx() {

        HttpClientFactory factory = clientApp.getInstance(HttpClientFactory.class);

        assertNull(TransactionIdMDC.getId());
        MDCTester tester = new MDCTester();

        CompletionStage<Response> rRx = factory
                .newClient()
                .register(tester)

                .target(jetty.getUrl() + "/get")
                .request()
                .rx()
                .get();

        rRx.toCompletableFuture().join().close();
        tester.expectTxIds(Map.of());
    }

    @Test
    public void testMDCRx() {

        HttpClientFactory factory = clientApp.getInstance(HttpClientFactory.class);

        TransactionIdMDC.setId("TEST_MDC");

        MDCTester tester = new MDCTester();

        CompletionStage<Response> rRx = factory
                .newClient()
                .register(tester)
                .target(jetty.getUrl() + "/get")
                .request()
                .rx()
                .get();

        rRx.toCompletableFuture().join().close();
        tester.expectTxIds(Map.of(0, "TEST_MDC"));
    }

    @Test
    public void testMDCRxNestedAsync() {

        HttpClientFactory factory = clientApp.getInstance(HttpClientFactory.class);

        TransactionIdMDC.setId("TEST_MDC");

        MDCTester tester = new MDCTester();
        WebTarget target = factory.newClient().register(tester).target(jetty.getUrl() + "/get");

        CompletionStage<CompletionStage<Response>> rRx = target
                .request()
                .rx()
                .get()
                .thenApply(r -> target.request().rx().get());

        rRx.toCompletableFuture().join().toCompletableFuture().join().close();
        tester.expectTxIds(Map.of(0, "TEST_MDC", 1, "TEST_MDC"));
    }

    @Test
    public void testMDCRxNestedSync() {

        HttpClientFactory factory = clientApp.getInstance(HttpClientFactory.class);

        TransactionIdMDC.setId("TEST_MDC");

        MDCTester tester = new MDCTester();
        WebTarget target = factory.newClient().register(tester).target(jetty.getUrl() + "/get");

        CompletionStage<Response> rRx = target
                .request()
                .rx()
                .get()
                .thenApply(r -> target.request().get());

        rRx.toCompletableFuture().join().close();
        tester.expectTxIds(Map.of(0, "TEST_MDC", 1, "TEST_MDC"));
    }

    @Test
    public void testMDCRxParallel() throws ExecutionException, InterruptedException, TimeoutException {

        HttpClientFactory factory = clientApp.getInstance(HttpClientFactory.class);

        List<Future<?>> tasks = new ArrayList<>();

        ExecutorService threadPool = Executors.newFixedThreadPool(5);

        try {
            for (int i = 0; i < 3; i++) {

                String txId = "id-" + i;

                Future<?> f = threadPool.submit(() -> {
                    TransactionIdMDC.setId(txId);

                    MDCTester tester = new MDCTester();
                    WebTarget target = factory.newClient().register(tester).target(jetty.getUrl() + "/get");

                    CompletionStage<Response> rRx = target
                            .request()
                            .rx()
                            .get()
                            .thenApply(r -> target.request().get());

                    rRx.toCompletableFuture().join().close();
                    tester.expectTxIds(Map.of(0, txId, 1, txId));
                });

                tasks.add(f);
            }

            for (Future<?> f : tasks) {
                f.get(1, TimeUnit.SECONDS);
            }
        } finally {
            threadPool.shutdown();
        }
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        @Path("get")
        public String get() {
            return "got";
        }
    }

    static class MDCTester implements ClientRequestFilter {

        final AtomicInteger counter = new AtomicInteger();
        final Map<Integer, String> txIds = new ConcurrentHashMap<>();

        @Override
        public void filter(ClientRequestContext requestContext) {
            if (TransactionIdMDC.getId() != null) {
                txIds.put(counter.getAndIncrement(), TransactionIdMDC.getId());
            }
        }

        public void expectTxIds(Map<Integer, String> expected) {
            assertEquals(expected, txIds);
        }
    }
}
