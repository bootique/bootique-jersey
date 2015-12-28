package com.nhl.bootique.jersey.unit;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.nhl.bootique.jersey.JerseyModule;

/**
 * An abstract superclass of Bootique Jersey integration tests.
 */
public abstract class BQJerseyTest extends JerseyTest {

	protected Injector injector;

	@Override
	protected Application configure() {
		this.injector = Guice.createInjector(createJerseyModule(), createTestModule());
		return injector.getInstance(ResourceConfig.class);
	}

	protected JerseyModule createJerseyModule() {
		return new JerseyModule();
	}

	/**
	 * Returns an empty module. Intends to be overridden.
	 * 
	 * @return an empty module.
	 */
	protected Module createTestModule() {
		return (b) -> {
		};
	}
}
