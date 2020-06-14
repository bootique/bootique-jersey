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
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class ProviderInjectionIT {

    @BQApp
    static final BQRuntime server = Bootique.app("--server")
            .modules(JettyModule.class, JerseyModule.class)
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .module(JettyTester.moduleReplacingConnectors())
            .createRuntime();

    @BQApp(skipRun = true)
    static final BQRuntime client = Bootique.app()
            .module(JerseyClientModule.class)
            .module(b -> {
                JerseyClientModule.extend(b).addFeature(TestResponseReaderFeature.class);
                b.bind(InjectedService.class);
            })
            .createRuntime();

    @Test
    public void testResponse() {

        WebTarget target = client
                .getInstance(HttpClientFactory.class)
                .newClient()
                .target(JettyTester.getUrl(server));

        Response r1 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("[bare_string]_1", r1.readEntity(TestResponse.class).toString());
        r1.close();

        Response r2 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("[bare_string]_2", r2.readEntity(TestResponse.class).toString());
        r2.close();
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        public String get() {
            return "bare_string";
        }
    }

    public static class TestResponse {

        private String string;

        public TestResponse(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    public static class InjectedService {

        private AtomicInteger atomicInt = new AtomicInteger();

        public int getNext() {
            return atomicInt.incrementAndGet();
        }
    }

    public static class TestResponseReaderFeature implements Feature {
        @Override
        public boolean configure(FeatureContext context) {
            context.register(TestResponseReader.class);
            return true;
        }
    }

    @Provider
    public static class TestResponseReader implements MessageBodyReader<TestResponse> {

        private InjectedService service;

        @Inject
        public TestResponseReader(InjectedService service) {
            this.service = service;
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type.equals(TestResponse.class);
        }

        @Override
        public TestResponse readFrom(Class<TestResponse> type, Type genericType, Annotation[] annotations,
                                     MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {

            String responseLine;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(entityStream, StandardCharsets.UTF_8))) {
                responseLine = in.readLine();
            }

            String s = String.format("[%s]_%s", responseLine, service.getNext());
            return new TestResponse(s);
        }
    }

}
