package com.nhl.bootique.jersey;

import static org.junit.Assert.assertNotNull;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Test;

import com.nhl.bootique.jersey.unit.BQJerseyTest;

public class JerseyModuleIT extends BQJerseyTest {

	@Test
	public void testDefaultContents() {
		assertNotNull(injector.getInstance(ResourceConfig.class));
		assertNotNull(injector.getInstance(ServletContainer.class));
	}
}
