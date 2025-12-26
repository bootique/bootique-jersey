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
import io.bootique.jersey.p1.PackageRegisteredApi;
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
public class EndpointScopeIT {

    static final JettyTester tester = JettyTester.create();

    @BQApp
    static final BQRuntime app = io.bootique.Bootique.app("--server")
            .autoLoadModules()
            .module(tester.moduleReplacingConnectors())
            .module(TestModule.class)
            .createRuntime();


    @Test
    public void implicitApi() {
        Response r1 = tester.getTarget().path("implicit").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("implicit").request().get();
        JettyTester.assertOk(r2).assertContent("0");
    }

    @Test
    public void implicitApiSingleton() {
        Response r1 = tester.getTarget().path("implicit-singleton").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("implicit-singleton").request().get();
        JettyTester.assertOk(r2).assertContent("1");
    }

    @Test
    public void implicitPathOverrideApi() {
        Response r1 = tester.getTarget().path("implicit-path-override-X").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("implicit-path-override-X").request().get();
        JettyTester.assertOk(r2).assertContent("0");
    }

    @Test
    public void implicitPathOverrideApiSingleton() {
        Response r1 = tester.getTarget().path("implicit-path-override-singleton-Y").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("implicit-path-override-singleton-Y").request().get();
        JettyTester.assertOk(r2).assertContent("1");
    }

    @Test
    public void providedApi() {
        Response r1 = tester.getTarget().path("provided").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("provided").request().get();
        JettyTester.assertOk(r2).assertContent("0");
    }

    @Test
    public void providedApiSingleton1() {
        Response r1 = tester.getTarget().path("provided-singleton-ignored").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("provided-singleton-ignored").request().get();
        JettyTester.assertOk(r2).assertContent("0");
    }

    @Test
    public void providedApiSingleton2() {
        Response r1 = tester.getTarget().path("provided-singleton").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("provided-singleton").request().get();
        JettyTester.assertOk(r2).assertContent("1");
    }

    @Test
    public void packageManagedApi() {
        Response r1 = tester.getTarget().path("package").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("package").request().get();
        JettyTester.assertOk(r2).assertContent("0");
    }

    @Test
    public void packageManagedApiSingleton() {
        Response r1 = tester.getTarget().path("package-singleton").request().get();
        JettyTester.assertOk(r1).assertContent("0");

        Response r2 = tester.getTarget().path("package-singleton").request().get();
        JettyTester.assertOk(r2).assertContent("1");
    }

    public static class TestModule implements BQModule {
        @Override
        public void configure(Binder binder) {

            JerseyModule.extend(binder)

                    .addApiResource(ImplicitApi.class)
                    .addApiResource(ImplicitApiSingleton.class)

                    .addApiResource(ImplicitPathOverrideApi.class, "implicit-path-override-X")
                    .addApiResource(ImplicitPathOverrideApiSingleton.class, "implicit-path-override-singleton-Y")

                    .addApiResource(ProvidedApi.class)
                    .addApiResource(ProvidedApiSingletonIgnored.class)
                    .addApiResource(ProvidedApiSingleton.class)

                    .addPackage(PackageRegisteredApi.class);
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

    @Path("implicit")
    public static class ImplicitApi {

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

    @Path("provided")
    public static class ProvidedApi {

        int counter;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return String.valueOf(counter++);
        }
    }

    // No @Singleton annotation is placed on the provides* method, so class @Singleton will be ignored
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

    @Path("implicit-path-override")
    public static class ImplicitPathOverrideApi {

        int counter;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return String.valueOf(counter++);
        }
    }

    @Singleton
    @Path("implicit-path-override-singleton")
    public static class ImplicitPathOverrideApiSingleton {

        int counter;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return String.valueOf(counter++);
        }
    }
}
