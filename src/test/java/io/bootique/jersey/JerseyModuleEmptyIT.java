package io.bootique.jersey;

import io.bootique.jetty.test.junit.JettyTestFactory;
import io.bootique.Bootique;
import io.bootique.test.BQDaemonTestRuntime;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.assertNotNull;

public class JerseyModuleEmptyIT {

	@Rule
	public JettyTestFactory jettyFactory = new JettyTestFactory();

	@Test
	public void testLoadNoResources() {

		Consumer<Bootique> configurator = b -> b.modules(JerseyModule.class);
		BQDaemonTestRuntime daemon = jettyFactory.newRuntime().configurator(configurator).startServer();
		assertNotNull(daemon.getRuntime().getInstance(ResourceConfig.class));
	}
}
