package com.nhl.bootique.jersey;

import static org.junit.Assert.assertNotNull;

import java.util.function.Consumer;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import com.nhl.bootique.BQRuntime;
import com.nhl.bootique.Bootique;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.test.BQTestRuntime;

public class JerseyModuleEmptyIT {

	@Test
	public void testLoadNoResources() {

		Consumer<Bootique> configurator = b -> b.modules(JettyModule.class, JerseyModule.class);
		BQRuntime runtime = new BQTestRuntime(configurator).createRuntime("--server");
		try {
			assertNotNull(runtime.getInstance(ResourceConfig.class));
		} finally {
			runtime.shutdown();
		}
	}
}
