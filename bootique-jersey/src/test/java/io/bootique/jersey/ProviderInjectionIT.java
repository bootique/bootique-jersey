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

import io.bootique.test.junit.BQTestFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ProviderInjectionIT {

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory().autoLoadModules();

    private static final WebTarget target = ClientBuilder.newClient().target("http://127.0.0.1:8080/");

    @BeforeClass
    public static void startJetty() {

        TEST_FACTORY.app("-s")
                .module(b -> {
                    b.bind(InjectedService.class).inSingletonScope();
                    JerseyModule.extend(b).addFeature(StringWriterFeature.class).addResource(Resource.class);
                })
                .run();
    }

    @Test
    public void testResponse() {

        Response r1 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("[bare_string]_1", r1.readEntity(String.class));
        r1.close();

        Response r2 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
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
