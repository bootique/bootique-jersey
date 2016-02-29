package com.nhl.bootique.jersey;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nhl.bootique.Bootique;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.test.BQDaemonTestRuntime;

public class ResourceInjectionIT {

	private static BQDaemonTestRuntime APP;

	private Client client;

	@BeforeClass
	public static void startJetty() throws InterruptedException, ExecutionException, TimeoutException {

		Consumer<Bootique> configurator = b -> {
			b.module(JettyModule.class);
			b.module(JerseyModule.builder().resource(InjectedResource.class).build());
			b.module(binder -> binder.bind(InjectedService.class).in(Singleton.class));
		};

		APP = new BQDaemonTestRuntime(configurator, r -> r.getInstance(Server.class).isStarted());
		APP.start(5, TimeUnit.SECONDS, "--server");
	}

	@AfterClass
	public static void stopJetty() throws InterruptedException {
		APP.stop();
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
