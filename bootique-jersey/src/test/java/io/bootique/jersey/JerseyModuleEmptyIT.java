package io.bootique.jersey;

import io.bootique.jetty.test.junit.JettyTestFactory;
import io.bootique.test.BQDaemonTestRuntime;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class JerseyModuleEmptyIT {

	@Rule
	public JettyTestFactory jettyFactory = new JettyTestFactory();

	@Test
	public void testLoadNoResources() {
		BQDaemonTestRuntime daemon = jettyFactory.app().module(JerseyModule.class).start();
		assertNotNull(daemon.getRuntime().getInstance(ResourceConfig.class));
	}
}
