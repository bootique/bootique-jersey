package io.bootique.jersey.jackson;

import io.bootique.jersey.JerseyModule;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class BQJerseyJacksonIT {

    @ClassRule
    public static JettyTestFactory JETTY_FACTORY = new JettyTestFactory();

    private Client client;

    @BeforeClass
    public static void startJetty() throws InterruptedException, ExecutionException, TimeoutException {
        JETTY_FACTORY.app()
                .autoLoadModules()
                .module(binder -> JerseyModule.extend(binder).addResource(JsonResource.class))
                .start();
    }

    @Before
    public void before() {
        ClientConfig config = new ClientConfig();
        this.client = ClientBuilder.newClient(config);
    }

    @Test
    public void testJacksonSerialization() {
        WebTarget target = client.target("http://127.0.0.1:8080/");

        Response r1 = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("{\"p1\":\"s\",\"p2\":45}", r1.readEntity(String.class));
        r1.close();
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static class JsonResource {

        @GET
        public Model get() {
            return new Model("s", 45);
        }
    }

    public static class Model {
        private String p1;
        private int p2;

        public Model(String p1, int p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        public String getP1() {
            return p1;
        }

        public int getP2() {
            return p2;
        }
    }
}
