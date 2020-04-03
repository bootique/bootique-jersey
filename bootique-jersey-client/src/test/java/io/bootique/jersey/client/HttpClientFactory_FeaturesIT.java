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

import io.bootique.di.BQModule;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpClientFactory_FeaturesIT {

    @ClassRule
    public static BQTestFactory SERVER_FACTORY = new BQTestFactory();

    @Rule
    public BQTestFactory CLIENT_FACTORY = new BQTestFactory();

    private HttpClientFactory clientFactory;

    @BeforeClass
    public static void beforeClass() {
        SERVER_FACTORY.app("--server")
                .modules(JettyModule.class, JerseyModule.class)
                .module(b -> JerseyModule.extend(b).addResource(Resource.class))
                .run();
    }

    @Before
    public void before() {

        BQModule features = binder -> JerseyClientModule
                .extend(binder)
                .addFeature(Feature1.class)
                .addFeature(Feature2.class);

        clientFactory = CLIENT_FACTORY
                .app()
                .module(JerseyClientModule.class)
                .module(features)
                .createRuntime()
                .getInstance(HttpClientFactory.class);
    }

    @Test
    public void testFeaturesLoaded() {

        assertFalse(Feature1.LOADED);
        assertFalse(Feature2.LOADED);

        clientFactory.newClient().target("http://127.0.0.1:8080/").request().get().close();

        assertTrue(Feature1.LOADED);
        assertTrue(Feature2.LOADED);
    }

    static class Feature1 implements Feature {

        static boolean LOADED = false;

        @Override
        public boolean configure(FeatureContext c) {
            LOADED = true;
            return true;
        }
    }

    static class Feature2 implements Feature {

        static boolean LOADED = false;

        @Override
        public boolean configure(FeatureContext c) {
            LOADED = true;
            return true;
        }
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        @Path("get")
        public String get() {
            return "got";
        }
    }
}
