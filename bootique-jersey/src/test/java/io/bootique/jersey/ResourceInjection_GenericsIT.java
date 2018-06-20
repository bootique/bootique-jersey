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
import org.junit.Ignore;
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

import static org.junit.Assert.assertEquals;

public class ResourceInjection_GenericsIT {

    private static final S1<String> STRING_BOUND = new S1<>("sss");
    private static final S1<Integer> INT_BOUND = new S1<>(4);
    private static final Client CLIENT = ClientBuilder.newClient(new ClientConfig());

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    @Ignore
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

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/f");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("u_4_sss", r.readEntity(String.class));
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

        WebTarget target = CLIENT.target("http://127.0.0.1:8080/f");

        Response r = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("u_4_sss", r.readEntity(String.class));
        r.close();
    }

    @Path("/f")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FieldInjectedResource {

        @Inject
        private S1<Integer> intService;

        @Inject
        private S1<String> stringService;

        @GET
        public String get() {
            return "f_" + intService.asString() + "_" + stringService.asString();
        }
    }

    @Path("/f")
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
            return "u_" + intService.asString() + "_" + stringService.asString();
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
