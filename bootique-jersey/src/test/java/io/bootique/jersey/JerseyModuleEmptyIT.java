package io.bootique.jersey;

import io.bootique.BQRuntime;
import io.bootique.jetty.test.junit.JettyTestFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class JerseyModuleEmptyIT {

	@Rule
	public JettyTestFactory jettyFactory = new JettyTestFactory();

	@Test
	public void testLoadNoResources() {
		BQRuntime daemon = jettyFactory.app().module(JerseyModule.class).start();
		assertNotNull(daemon.getInstance(ResourceConfig.class));
	}
}
