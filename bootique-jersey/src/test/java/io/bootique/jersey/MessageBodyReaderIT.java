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

import io.bootique.BQModule;
import io.bootique.BQRuntime;
import io.bootique.di.BQInject;
import io.bootique.di.Injector;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import io.bootique.junit5.TestRuntumeBuilder;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class MessageBodyReaderIT {

    private static final String TEST_PROPERTY = "bq.test.label";

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    protected WebTarget startServer(BQModule... modules) {

        JettyTester jetty = JettyTester.create();

        TestRuntumeBuilder builder = testFactory
                .app("-s")
                .module(jetty.moduleReplacingConnectors());

        asList(modules).forEach(builder::module);

        BQRuntime server = builder.createRuntime();
        assertTrue(server.run().isSuccess());
        return jetty.getTarget();
    }

    @Test
    public void readerWithContextInjection() {

        WebTarget client = startServer(b -> JerseyModule.extend(b)
                .addResource(Resource.class)
                .addFeature(fc -> {
                    fc.property(TEST_PROPERTY, "x");
                    fc.register(MessageReaderWithContextInjection.class);
                    return false;
                }));

        Response ok = client.request().put(Entity.entity("m", MediaType.TEXT_PLAIN_TYPE));
        JettyTester.assertOk(ok).assertContent("x_m");
    }

    @Test
    public void readerWithBqInjection() {
        WebTarget client = startServer(
                b -> JerseyModule.extend(b)
                        .addResource(Resource.class)
                        .addResource(MessageReaderWithInjection.class),
                b -> b.bind(Service1.class));

        Response ok = client.request().put(Entity.entity("m", MediaType.TEXT_PLAIN_TYPE));
        JettyTester.assertOk(ok).assertContent("s1_m");
    }

    @Test
    public void readerWithDynamicBqInjection() {
        WebTarget client = startServer(
                b -> JerseyModule.extend(b)
                        .addResource(Resource.class)
                        .addResource(MessageReaderWithDynamicBeanInjection.class));

        Response ok = client.request().put(Entity.entity("m", MediaType.TEXT_PLAIN_TYPE));
        JettyTester.assertOk(ok).assertContent("b2[s2]_m");
    }

    @Test
    public void readerWithAllInjections() {
        WebTarget client = startServer(
                b -> JerseyModule.extend(b)
                        .addResource(Resource.class)
                        .addFeature(fc -> {
                            fc.property(TEST_PROPERTY, "x");
                            fc.register(MessageReaderWithAllInjections.class);
                            return false;
                        }),
                b -> b.bind(Service1.class));

        Response ok = client.request().put(Entity.entity("m", MediaType.TEXT_PLAIN_TYPE));
        JettyTester.assertOk(ok).assertContent("x_s1_b2[s2]_b2[s2]_s2_s2_m");
    }

    @Provider
    public static class MessageReaderWithContextInjection extends BaseMessageReader {

        private final String label;

        public MessageReaderWithContextInjection(@Context Configuration config) {
            // ensure the label can be passed from configuration, an HK injection works
            this.label = (String) config.getProperty(TEST_PROPERTY);
        }

        @Override
        protected String processText(String text) {
            return label + "_" + text;
        }
    }

    @Provider
    public static class MessageReaderWithInjection extends BaseMessageReader {

        @BQInject
        private jakarta.inject.Provider<Service1> service;

        @Override
        protected String processText(String text) {
            return service.get().getLabel() + "_" + text;
        }
    }

    @Provider
    public static class MessageReaderWithDynamicBeanInjection extends BaseMessageReader {

        @Inject
        private DynamicBean2 bean;

        @Override
        protected String processText(String text) {
            return bean.getLabel() + "_" + text;
        }
    }

    @Provider
    public static class MessageReaderWithAllInjections extends BaseMessageReader {

        private final String label;

        @BQInject
        private DynamicBean2 b1;

        @Inject
        private DynamicBean2 b2;

        @Inject
        private jakarta.inject.Provider<Service2> p2;

        @Inject
        private Injector injector;

        public MessageReaderWithAllInjections(@Context Configuration config) {
            // ensure the label can be passed from configuration, an HK injection works
            this.label = (String) config.getProperty(TEST_PROPERTY);
        }

        @Override
        protected String processText(String text) {
            return String.join("_", List.of(
                    label,

                    injector.getInstance(Service1.class).getLabel(),

                    b1.getLabel(),
                    b2.getLabel(),
                    p2.get().getLabel(),
                    injector.getInstance(Service2.class).getLabel(),

                    text
            ));
        }
    }

    @Path("/")
    public static class Resource {

        @PUT
        @Consumes(MediaType.TEXT_PLAIN)
        public Response put(Message message) {
            return Response.ok(message.getMessage(), MediaType.TEXT_PLAIN_TYPE).build();
        }
    }

    public static class Message {
        private final String message;

        public Message(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class Service1 {
        String getLabel() {
            return "s1";
        }
    }

    public static class Service2 {
        String getLabel() {
            return "s2";
        }
    }

    public static class DynamicBean2 {
        @Inject
        Service2 service2;

        String getLabel() {
            return "b2[" + service2.getLabel() + "]";
        }
    }

    static abstract class BaseMessageReader implements MessageBodyReader<Message> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Message.class.isAssignableFrom(type);
        }

        @Override
        public Message readFrom(
                Class<Message> type,
                Type genericType,
                Annotation[] annotations,
                MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders,
                InputStream entityStream) throws WebApplicationException {

            String text;
            try (Scanner scanner = new Scanner(entityStream, StandardCharsets.UTF_8)) {
                text = scanner.useDelimiter("\\A").next();
            }

            return new Message(processText(text));
        }

        protected abstract String processText(String text);
    }
}
