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

import io.bootique.di.BQInject;
import io.bootique.di.Injector;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class MessageBodyReaderIT {

    private static final String TEST_PROPERTY = "bq.test.label";
    private static final WebTarget target = ClientBuilder.newClient().target("http://127.0.0.1:8080/");

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testReaderWithContextInjection() {
        testFactory.app("-s")
                .module(b -> JerseyModule.extend(b)
                        .addResource(Resource.class)
                        .addFeature(fc -> {
                            fc.property(TEST_PROPERTY, "x");
                            fc.register(MessageReaderWithContextInjection.class);
                            return false;
                        }))
                .run();

        Response ok = target.request().put(Entity.entity("m", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, ok.getStatus());
        assertEquals("x_m", ok.readEntity(String.class));
    }

    @Test
    public void testReaderWithBqInjection() {
        testFactory.app("-s")
                .module(b -> JerseyModule.extend(b)
                        .addResource(Resource.class)
                        .addResource(MessageReaderWithInjection.class))
                .module(b -> b.bind(Service.class))
                .run();

        Response ok = target.request().put(Entity.entity("m", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, ok.getStatus());
        assertEquals("s_m", ok.readEntity(String.class));
    }

    @Test
    public void testReaderWithDynamicBqInjection() {
        testFactory.app("-s")
                .module(b -> JerseyModule.extend(b)
                        .addResource(Resource.class)
                        .addResource(MessageReaderWithDynamicBeanInjection.class))
                .run();

        Response ok = target.request().put(Entity.entity("m", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, ok.getStatus());
        assertEquals("b_m", ok.readEntity(String.class));
    }

    @Test
    public void testReaderWithAllInjections() {
        testFactory.app("-s")
                .module(b -> JerseyModule.extend(b)
                        .addResource(Resource.class)
                        .addFeature(fc -> {
                            fc.property(TEST_PROPERTY, "x");
                            fc.register(MessageReaderWithAllInjections.class);
                            return false;
                        }))
                .module(b -> b.bind(Service.class))
                .run();

        Response ok = target.request().put(Entity.entity("m", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, ok.getStatus());
        assertEquals("x_b_s_m_s", ok.readEntity(String.class));
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
        private javax.inject.Provider<Service> service;

        @Override
        protected String processText(String text) {
            return service.get().getLabel() + "_" + text;
        }
    }

    @Provider
    public static class MessageReaderWithDynamicBeanInjection extends BaseMessageReader {

        @Inject
        private DynamicBean bean;

        @Override
        protected String processText(String text) {
            return bean.getLabel() + "_" + text;
        }
    }

    @Provider
    public static class MessageReaderWithAllInjections extends BaseMessageReader {

        private final String label;

        @Inject
        private DynamicBean bean;

        @BQInject
        private javax.inject.Provider<Service> service;

        @Inject
        private Injector injector;

        public MessageReaderWithAllInjections(@Context Configuration config) {
            // ensure the label can be passed from configuration, an HK injection works
            this.label = (String) config.getProperty(TEST_PROPERTY);
        }

        @Override
        protected String processText(String text) {
            return label + "_" + bean.getLabel() + "_" + service.get().getLabel() + "_"
                    + text + "_" + injector.getInstance(Service.class).getLabel();
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

    public static class Service {
        String getLabel() {
            return "s";
        }
    }

    public static class DynamicBean {
        String getLabel() {
            return "b";
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
            try (Scanner scanner = new Scanner(entityStream, StandardCharsets.UTF_8.name())) {
                text = scanner.useDelimiter("\\A").next();
            }

            return new Message(processText(text));
        }

        protected abstract String processText(String text);
    }
}
