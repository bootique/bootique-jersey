package io.bootique.jersey;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class JerseyModuleProviderIT {

	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(JerseyModuleProvider.class);
	}
}
