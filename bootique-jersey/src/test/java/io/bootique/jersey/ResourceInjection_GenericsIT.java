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

import io.bootique.BQRuntime;
import io.bootique.di.*;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.TestRuntumeBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceInjection_GenericsIT {

    private static final S1<String> STRING_BOUND = new S1<>("sss");
    private static final S1<Integer> INT_BOUND = new S1<>(4);
    private static final int[] INT_ARRAY_BOUND = {1, 2, 3};
    private static final String[] STRING_ARRAY_BOUND = {"a, b, c"};

    @RegisterExtension
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    protected WebTarget startServer(BQModule... modules) {
        TestRuntumeBuilder builder = testFactory
                .app("-s")
                .module(JettyTester.moduleReplacingConnectors());

        asList(modules).forEach(builder::module);

        BQRuntime server = builder.createRuntime();
        assertTrue(server.run().isSuccess());
        return JettyTester.getTarget(server);
    }

    @Test
    public void testFieldInjected() {

        WebTarget client = startServer(b -> {
            b.bind(Key.get(new TypeLiteral<S1<String>>() {
            })).toInstance(STRING_BOUND);
            b.bind(Key.get(new TypeLiteral<S1<Integer>>() {
            })).toInstance(INT_BOUND);
            JerseyModule.extend(b).addResource(FieldInjectedResource.class);
        });

        Response r = client.path("if").request().get();
        JettyTester.assertOk(r).assertContent("if_4_sss");
    }

    @Test
    public void testInjectedWildcard() {

        WebTarget client = startServer(b -> {
            b.bind(Key.get(new TypeLiteral<S1<?>>() {
            })).toInstance(STRING_BOUND);
            JerseyModule.extend(b).addResource(InjectedResourceWildcard.class);
        });

        Response r = client.path("iw").request().get();
        JettyTester.assertOk(r).assertContent("iw_sss");
    }

    @Test
    public void testInjectedExtendedWildcard() {

        WebTarget client = startServer(b -> {
            b.bind(Key.get(new TypeLiteral<S1<? extends Object>>() {
            })).toInstance(STRING_BOUND);
            JerseyModule.extend(b).addResource(InjectedResourceExtendedWildcard.class);
        });

        Response r = client.path("iw").request().get();
        JettyTester.assertOk(r).assertContent("iw_sss");
    }

    @Test
    public void testInjectedArray() {

        WebTarget client = startServer(b -> {
            b.bind(String[].class).toInstance(STRING_ARRAY_BOUND);
            b.bind(int[].class).toInstance(INT_ARRAY_BOUND);

            JerseyModule.extend(b).addResource(FieldInjectedResourceArray.class);
        });

        Response r = client.path("ia").request().get();
        JettyTester.assertOk(r).assertContent("ia_[a, b, c]_[1, 2, 3]");
    }

    @Test
    public void testUnInjected() {

        WebTarget client = startServer(
                new UninjectedModule(),
                b -> {
                    b.bind(Key.get(new TypeLiteral<S1<String>>() {
                    })).toInstance(STRING_BOUND);
                    b.bind(Key.get(new TypeLiteral<S1<Integer>>() {
                    })).toInstance(INT_BOUND);
                    JerseyModule.extend(b).addResource(UnInjectedResource.class);
                });

        Response r = client.path("uf").request().get();
        JettyTester.assertOk(r).assertContent("uf_4_sss");
    }

    @Test
    public void testUnInjectedWildcard() {

        WebTarget client = startServer(
                new UninjectedResourceWildcardModule(),
                b -> {
                    b.bind(Key.get(new TypeLiteral<S1<?>>() {
                    })).toInstance(STRING_BOUND);
                    JerseyModule.extend(b).addResource(UnInjectedResourceWildcard.class);
                });

        Response r = client.path("uw").request().get();
        JettyTester.assertOk(r).assertContent("uw_sss");
    }

    @Test
    public void testUnInjectedExtendedWildcard() {

        WebTarget client = startServer(
                new UninjectedResourceWildcardModule(),
                b -> {
                    b.bind(Key.get(new TypeLiteral<S1<?>>() {
                    })).toInstance(STRING_BOUND);
                    JerseyModule.extend(b).addResource(UnInjectedResourceExtendedWildcard.class);
                });

        Response r = client.path("uw").request().get();
        JettyTester.assertOk(r).assertContent("uw_sss");
    }

    @Test
    public void testUnInjectedArray() {

        WebTarget client = startServer(
                new UninjectedResourceArrayModule(),
                b -> {
                    b.bind(String[].class).toInstance(STRING_ARRAY_BOUND);
                    b.bind(int[].class).toInstance(INT_ARRAY_BOUND);
                    JerseyModule.extend(b).addResource(FieldUnInjectedResourceArray.class);
                });

        Response r = client.path("ua").request().get();
        JettyTester.assertOk(r).assertContent("ua_[a, b, c]_[1, 2, 3]");
    }

    @Path("/if")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FieldInjectedResource {

        @Inject
        private S1<Integer> intService;

        @Inject
        private S1<String> stringService;

        @GET
        public String get() {
            return "if_" + intService.asString() + "_" + stringService.asString();
        }
    }

    @Path("/uf")
    @Produces(MediaType.TEXT_PLAIN)
    public static class UnInjectedResource {

        private S1<Integer> intService;
        private S1<String> stringService;

        public UnInjectedResource(S1<Integer> intService, S1<String> stringService) {
            this.intService = intService;
            this.stringService = stringService;
        }

        @GET
        public String get() {
            return "uf_" + intService.asString() + "_" + stringService.asString();
        }
    }

    @Path("/iw")
    @Produces(MediaType.TEXT_PLAIN)
    public static class InjectedResourceWildcard {
        @BQInject
        private S1<?> service;

        @GET
        public String get() {
            return "iw_" + service.asString();
        }
    }

    @Path("/iw")
    @Produces(MediaType.TEXT_PLAIN)
    public static class InjectedResourceExtendedWildcard {
        @BQInject
        private S1<? extends Object> service;

        @GET
        public String get() {
            return "iw_" + service.asString();
        }
    }

    @Path("/uw")
    @Produces(MediaType.TEXT_PLAIN)
    public static class UnInjectedResourceWildcard {
        private S1<?> service;

        public UnInjectedResourceWildcard(S1<?> service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "uw_" + service.asString();
        }
    }

    @Path("/uw")
    @Produces(MediaType.TEXT_PLAIN)
    public static class UnInjectedResourceExtendedWildcard {
        private S1<? extends Object> service;

        public UnInjectedResourceExtendedWildcard(S1<?> service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "uw_" + service.asString();
        }
    }

    @Path("/ia")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FieldInjectedResourceArray {
        @Inject
        private String[] stringArray;

        @Inject
        private int[] intArray;

        @GET
        public String get() {
            return "ia_" + Arrays.toString(stringArray) + "_" + Arrays.toString(intArray);
        }
    }

    @Path("/ua")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FieldUnInjectedResourceArray {
        private String[] stringArray;
        private int[] intArray;

        public FieldUnInjectedResourceArray(String[] stringArray, int[] intArray) {
            this.stringArray = stringArray;
            this.intArray = intArray;
        }

        @GET
        public String get() {
            return "ua_" + Arrays.toString(stringArray) + "_" + Arrays.toString(intArray);
        }
    }

    public static class UninjectedModule implements BQModule {
        @Override
        public void configure(Binder binder) {
        }

        @Provides
        @Singleton
        UnInjectedResource provideUninjectedResource(S1<Integer> intService, S1<String> stringService) {
            return new UnInjectedResource(intService, stringService);
        }
    }

    public static class UninjectedResourceWildcardModule implements BQModule {
        @Override
        public void configure(Binder binder) {
        }

        @Provides
        @Singleton
        UnInjectedResourceWildcard provideUnInjectedResourceWildcard(S1<?> service) {
            return new UnInjectedResourceWildcard(service);
        }

        @Provides
        @Singleton
        UnInjectedResourceExtendedWildcard provideUnInjectedResourceExtendedWildcard(S1<? extends Object> service) {
            return new UnInjectedResourceExtendedWildcard(service);
        }

    }

    public static class UninjectedResourceArrayModule implements BQModule {
        @Override
        public void configure(Binder binder) {
        }

        @Provides
        @Singleton
        FieldUnInjectedResourceArray provideFieldUnInjectedResourceArray(String[] stringArray, int[] intArray) {
            return new FieldUnInjectedResourceArray(stringArray, intArray);
        }

    }

    public static class S1<T> {

        private T t;

        public S1(T t) {
            this.t = t;
        }

        public String asString() {
            return t.toString();
        }
    }
}
