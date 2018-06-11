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

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.test.junit.BQTestFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ResourceInjection_GenericsIT {

    private static final S1<String> STRING_BOUND = new S1<>("sss");
    private static final S1<Integer> INT_BOUND = new S1<>(4);
    private static final int[] INT_ARRAY_BOUND = {1, 2, 3};
    private static final String[] STRING_ARRAY_BOUND = {"a, b, c"};
    private static final Client CLIENT = ClientBuilder.newClient(new ClientConfig());

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testFieldInjected() {

        testFactory.app("-s")
                .module(binder -> {
                    binder.bind(new TypeLiteral<S1<String>>() {
                    }).toInstance(STRING_BOUND);
                    binder.bind(new TypeLiteral<S1<Integer>>() {
                    }).toInstance(INT_BOUND);
                    JerseyModule.extend(binder).addResource(FieldInjectedResource.class);
                })
                .run();

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/if");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("if_4_sss", r.readEntity(String.class));
        r.close();
    }

    @Test
    public void testInjectedWildcard() {

        testFactory.app("-s")
                .module(binder -> {
                    binder.bind(new TypeLiteral<S1<?>>() {
                    }).toInstance(STRING_BOUND);
                    JerseyModule.extend(binder).addResource(InjectedResourceWildcard.class);
                })
                .run();

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/iw");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("iw_sss", r.readEntity(String.class));
        r.close();
    }

    @Test
    public void testInjectedExtendedWildcard() {

        testFactory.app("-s")
                .module(binder -> {
                    binder.bind(new TypeLiteral<S1<? extends Object>>() {
                    }).toInstance(STRING_BOUND);
                    JerseyModule.extend(binder).addResource(InjectedResourceExtendedWildcard.class);
                })
                .run();

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/iw");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("iw_sss", r.readEntity(String.class));
        r.close();
    }

    @Test
    public void testInjectedArray() {

        testFactory.app("-s")
                .module(binder -> {
                    binder.bind(String[].class).toInstance(STRING_ARRAY_BOUND);
                    binder.bind(int[].class).toInstance(INT_ARRAY_BOUND);

                    JerseyModule.extend(binder).addResource(FieldInjectedResourceArray.class);
                })
                .run();

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/ia");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("ia_[a, b, c]_[1, 2, 3]", r.readEntity(String.class));
        r.close();
    }

    @Test
    public void testUnInjected() {

        testFactory.app("-s")
                .module(UninjectedModule.class)
                .module(binder -> {
                    binder.bind(new TypeLiteral<S1<String>>() {
                    }).toInstance(STRING_BOUND);
                    binder.bind(new TypeLiteral<S1<Integer>>() {
                    }).toInstance(INT_BOUND);
                    JerseyModule.extend(binder).addResource(UnInjectedResource.class);
                })
                .run();

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/uf");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("uf_4_sss", r.readEntity(String.class));
        r.close();
    }

    @Test
    public void testUnInjectedWildcard() {

        testFactory.app("-s")
                .module(UninjectedResourceWildcardModule.class)
                .module(binder -> {
                    binder.bind(new TypeLiteral<S1<?>>() {
                    }).toInstance(STRING_BOUND);
                    JerseyModule.extend(binder).addResource(UnInjectedResourceWildcard.class);
                })
                .run();

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/uw");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("uw_sss", r.readEntity(String.class));
        r.close();
    }

    @Test
    public void testUnInjectedExtendedWildcard() {

        testFactory.app("-s")
                .module(UninjectedResourceWildcardModule.class)
                .module(binder -> {
                    binder.bind(new TypeLiteral<S1<?>>() {
                    }).toInstance(STRING_BOUND);
                    JerseyModule.extend(binder).addResource(UnInjectedResourceExtendedWildcard.class);
                })
                .run();

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/uw");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("uw_sss", r.readEntity(String.class));
        r.close();
    }

    @Test
    public void testUnInjectedArray() {

        testFactory.app("-s")
                .module(UninjectedResourceArrayModule.class)
                .module(binder -> {
                    binder.bind(String[].class).toInstance(STRING_ARRAY_BOUND);
                    binder.bind(int[].class).toInstance(INT_ARRAY_BOUND);
                    JerseyModule.extend(binder).addResource(FieldUnInjectedResourceArray.class);
                })
                .run();

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/ua");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("ua_[a, b, c]_[1, 2, 3]", r.readEntity(String.class));
        r.close();
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
        @Inject
        private S1<?> service;

        @GET
        public String get() {
            return "iw_" + service.asString();
        }
    }

    @Path("/iw")
    @Produces(MediaType.TEXT_PLAIN)
    public static class InjectedResourceExtendedWildcard {
        @Inject
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

    public static class UninjectedModule implements Module {
        @Override
        public void configure(Binder binder) {
        }

        @Provides
        @Singleton
        UnInjectedResource provideUninjectedResource(S1<Integer> intService, S1<String> stringService) {
            return new UnInjectedResource(intService, stringService);
        }
    }

    public static class UninjectedResourceWildcardModule implements Module {
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

    public static class UninjectedResourceArrayModule implements Module {
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
