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

package io.bootique.jersey.v3;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.v11.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class ProviderInjectionIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> b.bind(InjectedService.class).inSingletonScope())
            .module(b -> JerseyModule.extend(b).addFeature(StringWriterFeature.class).addResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @Test
    public void testResponse() {

        WebTarget client = jetty.getTarget();

        Response r1 = client.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("[bare_string]_1", r1.readEntity(String.class));
        r1.close();

        Response r2 = client.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("[bare_string]_2", r2.readEntity(String.class));
        r2.close();
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        public TestResponse get() {
            return new TestResponse("bare_string");
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

    public static class StringWriterFeature implements Feature {
        @Override
        public boolean configure(FeatureContext context) {
            context.register(TestResponseWriter.class);
            return true;
        }
    }

    @Provider
    public static class TestResponseWriter implements MessageBodyWriter<TestResponse> {

        private InjectedService service;

        @Inject
        public TestResponseWriter(InjectedService service) {
            this.service = service;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type.equals(TestResponse.class);
        }

        @Override
        public long getSize(TestResponse t, Class<?> type, Type genericType, Annotation[] annotations,
                            MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(TestResponse t, Class<?> type, Type genericType, Annotation[] annotations,
                            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {

            String s = String.format("[%s]_%s", t, service.getNext());
            entityStream.write(s.getBytes());
        }
    }

}
