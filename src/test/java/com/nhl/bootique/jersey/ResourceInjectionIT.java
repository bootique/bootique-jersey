package com.nhl.bootique.jersey;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nhl.bootique.Bootique;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.jetty.test.junit.JettyTestFactory;

public class ResourceInjectionIT {

	@ClassRule
	public static JettyTestFactory JETTY_FACTORY = new JettyTestFactory();

	private Client client;

	@BeforeClass
	public static void startJetty() throws InterruptedException, ExecutionException, TimeoutException {

		Consumer<Bootique> configurator = b -> {
			b.modules(JettyModule.class, JerseyModule.class);
			b.module(binder -> {
				binder.bind(InjectedService.class).in(Singleton.class);
				JerseyModule.contributeResources(binder).addBinding().to(InjectedResource.class);
			});
		};

		JETTY_FACTORY.newRuntime().configurator(configurator).startServer();
	}

	@Before
	public void before() {
		ClientConfig config = new ClientConfig();
		this.client = ClientBuilder.newClient(config);
	}

	@Test
	public void testResponse() {

		WebTarget target = client.target("http://127.0.0.1:8080/");

		Response r1 = target.request().get();
		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals("_1", r1.readEntity(String.class));
		r1.close();

		Response r2 = target.request().get();
		assertEquals(Status.OK.getStatusCode(), r2.getStatus());
		assertEquals("_2", r2.readEntity(String.class));
		r2.close();
	}

	@Path("/")
	@Produces(MediaType.TEXT_PLAIN)
	public static class InjectedResource {

		@Inject
		private InjectedService service;

		@GET
		public String get() {
			return "_" + service.getNext();
		}
	}

	public static class InjectedService {

		private AtomicInteger atomicInt = new AtomicInteger();

		public int getNext() {
			return atomicInt.incrementAndGet();
		}
	}
}
