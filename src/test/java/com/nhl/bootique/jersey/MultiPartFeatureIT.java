package com.nhl.bootique.jersey;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Module;
import com.nhl.bootique.jersey.unit.TestJettyApp;
import com.nhl.bootique.jetty.JettyModule;

// see https://github.com/nhl/bootique-jersey/issues/11
public class MultiPartFeatureIT {

	private static TestJettyApp app;

	private Client multiPartClient;

	@BeforeClass
	public static void startJetty() throws InterruptedException, ExecutionException, TimeoutException {
		app = new TestJettyApp(b -> {
			b.modules(JettyModule.class).modules(createTestModule(), createJerseyModule());
		});

		app.startAndWait(5000, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void stopJetty() throws InterruptedException {
		app.stop();
	}

	protected static Module createTestModule() {
		return (b) -> {
			JerseyModule.contributeFeatures(b).addBinding().to(MultiPartFeature.class);
		};
	}

	protected static Module createJerseyModule() {
		return JerseyModule.builder().resource(Resource.class).build();
	}

	@Before
	public void before() {
		ClientConfig config = new ClientConfig();
		config.register(MultiPartFeature.class);
		this.multiPartClient = ClientBuilder.newClient(config);
	}

	@Test
	public void testResponse() {

		FormDataBodyPart part = new FormDataBodyPart("upload", "I am a part", MediaType.TEXT_PLAIN_TYPE);
		FormDataMultiPart multipart = new FormDataMultiPart();
		multipart.bodyPart(part);

		Response r = multiPartClient.target("http://127.0.0.1:8080/").request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(multipart, multipart.getMediaType()));
		
		assertEquals(Status.OK.getStatusCode(), r.getStatus());
		assertEquals("{\"message\":\"I am a part\"}", r.readEntity(String.class));
		
		r.close();
	}

	@Path("/")
	public static class Resource {

		@POST
		@Produces(MediaType.APPLICATION_JSON)
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		public Response uploadMultiPart(@FormDataParam("upload") String upload) {
			return Response.ok().entity("{\"message\":\"" + upload + "\"}").build();
		}
	}
}
