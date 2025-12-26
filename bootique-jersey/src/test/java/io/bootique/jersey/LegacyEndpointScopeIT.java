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
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

@BQTest
@Deprecated
public class LegacyEndpointScopeIT {

    static final JettyTester tester = JettyTester.create();

    @BQApp
    static final BQRuntime app = io.bootique.Bootique.app("--server")
            .autoLoadModules()
            .module(tester.moduleReplacingConnectors())
            .module(TestModule.class)
            .createRuntime();

    @Test
    public void providedApi() {
        Response r1 = tester.getTarget().path("provided").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("provided").request().get();
        // incorrect scope, fixed in 4.0 in "addApiResource"
        JettyTester.assertOk(r2).assertContent("1");
    }

    @Test
    public void providedApiSingleton1() {
        Response r1 = tester.getTarget().path("provided-singleton-ignored").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("provided-singleton-ignored").request().get();

        // incorrect scope, fixed in 4.0 in "addApiResource"
        JettyTester.assertOk(r2).assertContent("1");
    }

    @Test
    public void providedApiSingleton2() {
        Response r1 = tester.getTarget().path("provided-singleton").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("provided-singleton").request().get();
        JettyTester.assertOk(r2).assertContent("1");
    }

    @Test
    public void implicitApi() {
        Response r1 = tester.getTarget().path("implicit").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("implicit").request().get();

        // incorrect scope, fixed in 4.0 in "addApiResource"
        JettyTester.assertOk(r2).assertContent("1");
    }

    @Test
    public void implicitApiSingleton() {
        Response r1 = tester.getTarget().path("implicit-singleton").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("implicit-singleton").request().get();
        JettyTester.assertOk(r2).assertContent("1");
    }

    public static class TestModule implements BQModule {
        @Override
        public void configure(Binder binder) {

            JerseyModule.extend(binder)
                    .addResource(ProvidedApi.class)
                    .addResource(ImplicitApi.class)
                    .addResource(ProvidedApiSingletonIgnored.class)
                    .addResource(ProvidedApiSingleton.class)
                    .addResource(ImplicitApiSingleton.class);
        }

        @Provides
        ProvidedApi provideApi() {
            return new ProvidedApi();
        }

        @Provides
        ProvidedApiSingletonIgnored provideApiSingleton1() {
            return new ProvidedApiSingletonIgnored();
        }

        @Singleton
        @Provides
        ProvidedApiSingleton provideApiSingleton2() {
            return new ProvidedApiSingleton();
        }
    }

    @Path("provided")
    public static class ProvidedApi {

        int counter;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return String.valueOf(counter++);
        }
    }

    @Path("implicit")
    public static class ImplicitApi {

        int counter;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return String.valueOf(counter++);
        }
    }

    // No @Singleton annotation is placed on the provides* method; class @Singleton will be ignore
    @Singleton
    @Path("provided-singleton-ignored")
    public static class ProvidedApiSingletonIgnored {

        int counter;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return String.valueOf(counter++);
        }
    }

    // @Singleton annotation is placed on the provides* method
    @Path("provided-singleton")
    public static class ProvidedApiSingleton {

        int counter;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return String.valueOf(counter++);
        }
    }

    @Singleton
    @Path("implicit-singleton")
    public static class ImplicitApiSingleton {

        int counter;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return String.valueOf(counter++);
        }
    }
}
