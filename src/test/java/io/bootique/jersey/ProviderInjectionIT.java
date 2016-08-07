package io.bootique.jersey;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.bootique.Bootique;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class ProviderInjectionIT {

	@ClassRule
	public static JettyTestFactory JETTY_FACTORY = new JettyTestFactory();

	private Client client;

	@BeforeClass
	public static void startJetty() throws InterruptedException, ExecutionException, TimeoutException {

		Consumer<Bootique> configurator = b -> {
			b.modules(JettyModule.class, JerseyModule.class);
			b.module(binder -> {
				binder.bind(InjectedService.class).in(Singleton.class);
				JerseyModule.contributeFeatures(binder).addBinding().to(StringWriterFeature.class);
				JerseyModule.contributeResources(binder).addBinding().to(Resource.class);
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
		assertEquals("[bare_string]_1", r1.readEntity(String.class));
		r1.close();

		Response r2 = target.request().get();
		assertEquals(Status.OK.getStatusCode(), r2.getStatus());
		assertEquals("[bare_string]_2", r2.readEntity(String.class));
		r2.close();
	}

	@Path("/")
	@Produces(MediaType.TEXT_PLAIN)
	public static class Resource {

		@GET
		public TestResponse get() {
			return new TestResponse("bare_string");
		}
	}

	public static class TestResponse {

		private String string;

		public TestResponse(String string) {
			this.string = string;
		}

		@Override
		public String toString() {
			return string;
		}
	}

	public static class InjectedService {

		private AtomicInteger atomicInt = new AtomicInteger();

		public int getNext() {
			return atomicInt.incrementAndGet();
		}
	}

	public static class StringWriterFeature implements Feature {
		@Override
		public boolean configure(FeatureContext context) {
			context.register(TestResponseWriter.class);
			return true;
		}
	}

	@Provider
	public static class TestResponseWriter implements MessageBodyWriter<TestResponse> {

		private InjectedService service;

		@Inject
		public TestResponseWriter(InjectedService service) {
			this.service = service;
		}

		@Override
		public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
			return type.equals(TestResponse.class);
		}

		@Override
		public long getSize(TestResponse t, Class<?> type, Type genericType, Annotation[] annotations,
				MediaType mediaType) {
			return -1;
		}

		@Override
		public void writeTo(TestResponse t, Class<?> type, Type genericType, Annotation[] annotations,
				MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
						throws IOException, WebApplicationException {

			String s = String.format("[%s]_%s", t, service.getNext());
			entityStream.write(s.getBytes());
		}
	}

}
