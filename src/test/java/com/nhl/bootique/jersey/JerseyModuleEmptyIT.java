package com.nhl.bootique.jersey;

import static org.junit.Assert.assertNotNull;

import java.util.function.Consumer;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;

import com.nhl.bootique.Bootique;
import com.nhl.bootique.jetty.test.junit.JettyTestFactory;
import com.nhl.bootique.test.BQDaemonTestRuntime;

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
