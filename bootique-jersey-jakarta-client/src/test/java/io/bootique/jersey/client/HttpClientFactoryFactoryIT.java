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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.di.Injector;
import io.bootique.jersey.JerseyModule;
import io.bootique.jersey.client.auth.AuthenticatorFactory;
import io.bootique.jersey.client.auth.BasicAuthenticatorFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@BQTest
public class HttpClientFactoryFactoryIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("--server")
            .modules(JettyModule.class, JerseyModule.class)
            .module(b -> JerseyModule.extend(b).addResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    private Injector mockInjector = mock(Injector.class);

    @Test
    public void testCreateClientFactory_FollowRedirect() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        factoryFactory.setFollowRedirects(true);
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider()).newClient();

        Response r = client.target(jetty.getUrl()).path("/302").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("got", r.readEntity(String.class));
    }

    @Test
    public void testCreateClientFactory_NoFollowRedirect() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        factoryFactory.setFollowRedirects(false);
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider()).newClient();

        Response r = client.target(jetty.getUrl()).path("/302").request().get();
        JettyTester.assertStatus(r, 307).assertHeader("location", jetty.getUrl() + "/get");
    }

    @Test
    public void testCreateClientFactory_DefaultRedirect_Follow() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider()).newClient();

        Response r = client.target(jetty.getUrl()).path("/302").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("got", r.readEntity(String.class));
    }

    @Test
    public void testCreateClientFactory_Compression() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        factoryFactory.setCompression(true);
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider()).newClient();

        Response r = client.target(jetty.getUrl()).path("/getbig").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("gzip", r.getHeaderString("Content-Encoding"));
    }

    @Test
    public void testCreateClientFactory_NoCompression() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        factoryFactory.setCompression(false);
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider()).newClient();

        Response r = client.target(jetty.getUrl()).path("/getbig").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertNull(r.getHeaderString("Content-Encoding"));
    }

    @Test
    public void testCreateClientFactory_CompressionDefault() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        factoryFactory.setCompression(true);
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider()).newClient();

        Response r = client.target(jetty.getUrl()).path("/getbig").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("gzip", r.getHeaderString("Content-Encoding"));
    }

    @Test
    public void testCreateClientFactory_NoTimeout() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider()).newClient();

        Response r = client.target(jetty.getUrl()).path("/slowget").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("slowly_got", r.readEntity(String.class));
    }

    @Test
    public void testCreateClientFactory_LongTimeout() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        factoryFactory.setReadTimeoutMs(2000);
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider()).newClient();

        Response r = client.target(jetty.getUrl()).path("/slowget").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("slowly_got", r.readEntity(String.class));
    }

    @Test
    public void testCreateClientFactory_ReadTimeout() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();
        factoryFactory.setReadTimeoutMs(50);
        Client client = factoryFactory.createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider()).newClient();

        assertThrows(ProcessingException.class,
                () -> client.target(jetty.getUrl()).path("/slowget").request().get());
    }

    @Test
    public void testCreateClientFactory_BasicAuth() {

        HttpClientFactoryFactory factoryFactory = new HttpClientFactoryFactory();

        BasicAuthenticatorFactory authenticator = new BasicAuthenticatorFactory();
        authenticator.setPassword("p1");
        authenticator.setUsername("u1");

        Map<String, AuthenticatorFactory> auth = new HashMap<>();
        auth.put("a1", authenticator);
        factoryFactory.setAuth(auth);

        Client client = factoryFactory
                .createClientFactory(mockInjector, Collections.emptySet(), new HttpUrlConnectorProvider())
                .newBuilder().auth("a1")
                .build();

        Response r = client.target(jetty.getUrl()).path("/basicget").request().get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("got_basic_Basic dTE6cDE=", r.readEntity(String.class));
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public static class Resource {

        @GET
        @Path("get")
        public String get() {
            return "got";
        }

        @GET
        @Path("getbig")
        // value big enough to ensure compression kicks in
        public String getBig() {
            return "gotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgotgot";
        }

        @GET
        @Path("302")
        public Response threeOhTwo() throws URISyntaxException {
            return Response.temporaryRedirect(new URI("/get")).build();
        }

        @GET
        @Path("slowget")
        public String slowGet() throws InterruptedException {
            Thread.sleep(1000);
            return "slowly_got";
        }

        @GET
        @Path("basicget")
        public String basicGet(@HeaderParam("Authorization") String auth) {
            return "got_basic_" + auth;
        }
    }
}
