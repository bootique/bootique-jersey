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

import io.bootique.BQCoreModule;
import io.bootique.di.BQModule;
import io.bootique.di.Injector;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.logback.LogbackModule;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@BQTest
public class HttpClientFactoryFactory_LoggingIT {

    @BQTestTool
    final BQTestFactory serverFactory = new BQTestFactory();

    private Injector mockInjector;
    private File logsDir;

    private void startApp(String config) {

        BQModule extensions = b -> {
            JerseyModule.extend(b).addResource(Resource.class);

            // TODO: this test is seriously dirty.. we don't start the client from Bootique,
            // yet we reuse Bootique Logback configuration for client logging.
            // so here we are turning off logging from the server....
            BQCoreModule.extend(b)
                    .setLogLevel("org.eclipse.jetty.server", Level.OFF)
                    .setLogLevel("org.eclipse.jetty.util", Level.OFF);
        };

        serverFactory.app("--server", "--config=src/test/resources/io/bootique/jersey/client/" + config)
                .modules(JettyModule.class, JerseyModule.class, LogbackModule.class)
                .module(extensions)
                .run();
    }

    @BeforeEach
    public void before() {
        mockInjector = mock(Injector.class);
        logsDir = new File("target/logback");

        if (logsDir.exists()) {
            asList(logsDir.listFiles()).forEach(f -> f.delete());
        }
    }

    @Test
    public void testCreateClientFactory_Debug() throws IOException, InterruptedException {

        startApp("debug.yml");

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        factoryFactory.setFollowRedirects(true);
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet()).newClient();

        Response r = client.target("http://127.0.0.1:8080/get").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("got", r.readEntity(String.class));

        // wait for the log file to be flushed... there seems to be a race
        // condition in CI, resulting in assertions below not seeing the full
        // log
        Thread.sleep(500);

        File log = new File(logsDir, "debug.log");
        List<String> lines = Files.readAllLines(log.toPath());
        assertEquals(3, lines.size(), lines.stream().collect(joining("\n")));
        assertTrue(lines.get(1).contains("GET http://127.0.0.1:8080/get"));
    }

    @Test
    public void testCreateClientFactory_Warn() throws IOException, InterruptedException {

        startApp("warn.yml");

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        factoryFactory.setFollowRedirects(true);
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet()).newClient();

        Response r = client.target("http://127.0.0.1:8080/get").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("got", r.readEntity(String.class));

        // wait for the log file to be flushed... there seems to be a race
        // condition in CI, resulting in assertions below not seeing the full
        // log
        Thread.sleep(500);

        File log = new File(logsDir, "warn.log");
        List<String> lines = Files.readAllLines(log.toPath());
        assertEquals(0, lines.size());
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
}
