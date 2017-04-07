package io.bootique.jersey;

import com.google.inject.Inject;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.test.junit.JettyTestFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ResourceInjectionIT {

    @ClassRule
    public static JettyTestFactory JETTY_FACTORY = new JettyTestFactory();

    private static InjectedService SERVICE;

    private Client client;

    @BeforeClass
    public static void startJetty() throws InterruptedException, ExecutionException, TimeoutException {

        SERVICE = new InjectedService();

        JETTY_FACTORY.app()
                .modules(JettyModule.class, JerseyModule.class)
                .module(binder -> {
                    binder.bind(InjectedService.class).toInstance(SERVICE);
                    JerseyModule.extend(binder)
                            .addResource(FieldInjectedResource.class)
                            .addResource(ConstructorInjectedResource.class)
                            .addResource(UnInjectedResource.class);

                    binder.bind(UnInjectedResource.class).toProvider(() -> new UnInjectedResource(SERVICE));

                }).start();
    }

    @Before
    public void before() {

        SERVICE.reset();

        ClientConfig config = new ClientConfig();
        this.client = ClientBuilder.newClient(config);
    }

    @Test
    public void testFieldInjected() {

        WebTarget target = client.target("http://127.0.0.1:8080/f");

        Response r1 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("f_1", r1.readEntity(String.class));
        r1.close();

        Response r2 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("f_2", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testConstructorInjected() {

        WebTarget target = client.target("http://127.0.0.1:8080/c");

        Response r1 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("c_1", r1.readEntity(String.class));
        r1.close();

        Response r2 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("c_2", r2.readEntity(String.class));
        r2.close();
    }

    @Test
    public void testProviderForResource() {

        WebTarget target = client.target("http://127.0.0.1:8080/u");

        Response r1 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("u_1", r1.readEntity(String.class));
        r1.close();

        Response r2 = target.request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        assertEquals("u_2", r2.readEntity(String.class));
        r2.close();
    }

    @Path("/f")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FieldInjectedResource {

        @Inject
        private InjectedService service;

        @GET
        public String get() {
            return "f_" + service.getNext();
        }
    }

    @Path("/c")
    @Produces(MediaType.TEXT_PLAIN)
    public static class ConstructorInjectedResource {

        private InjectedService service;

        @Inject
        public ConstructorInjectedResource(InjectedService service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "c_" + service.getNext();
        }
    }

    @Path("/u")
    @Produces(MediaType.TEXT_PLAIN)
    public static class UnInjectedResource {

        private InjectedService service;

        public UnInjectedResource(InjectedService service) {
            this.service = service;
        }

        @GET
        public String get() {
            return "u_" + service.getNext();
        }
    }

    public static class InjectedService {

        private AtomicInteger atomicInt = new AtomicInteger();

        public void reset() {
            atomicInt.set(0);
        }

        public int getNext() {
            return atomicInt.incrementAndGet();
        }
    }
}
