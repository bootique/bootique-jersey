package io.bootique.jersey;

import static org.junit.Assert.assertNotNull;

import io.bootique.jersey.unit.BQJerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Test;

public class JerseyModuleIT extends BQJerseyTest {

	@Test
	public void testDefaultContents() {
		assertNotNull(injector.getInstance(ResourceConfig.class));
		assertNotNull(injector.getInstance(ServletContainer.class));
	}
}
