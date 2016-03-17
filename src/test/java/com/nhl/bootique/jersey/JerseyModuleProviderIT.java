package com.nhl.bootique.jersey;

import org.junit.Test;

import com.nhl.bootique.test.junit.BQModuleProviderChecker;

public class JerseyModuleProviderIT {

	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(JerseyModuleProvider.class);
	}
}
