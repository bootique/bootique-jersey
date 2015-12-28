package com.nhl.bootique.jersey;

import javax.inject.Singleton;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import com.google.inject.Injector;

public class GuiceBridgeFeature implements Feature {

	private static final String INJECTOR_PROPERTY = "com.nhl.bootique.jersey.injector";

	// TODO: can all of this happen inside this Feature "configure(..)" method?
	static void register(ResourceConfig config, Injector injector) {
		config.property(GuiceBridgeFeature.INJECTOR_PROPERTY, injector);
		config.register(GuiceBridgeFeature.class);
	}

	static Injector getInjector(Configuration configuration) {
		Injector injector = (Injector) configuration.getProperty(GuiceBridgeFeature.INJECTOR_PROPERTY);
		if (injector == null) {
			throw new IllegalStateException("Injector is not available in JAX RS runtime. Use property '"
					+ GuiceBridgeFeature.INJECTOR_PROPERTY + "' to set it");
		}

		return injector;
	}

	@Override
	public boolean configure(FeatureContext context) {

		// for now only supporting Guice injection to resources

		// TODO: if we want to inject web environment objects back into Guice
		// services or to use JSR-330 annotations in Resources, we need
		// org.glassfish.hk2:guice-bridge integration

		// This feature can inject HK2 ServiceLocator in constructor, and then
		// we can bridge it both ways with Guice

		context.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(GuiceInjectInjector.class).to(new TypeLiteral<InjectionResolver<com.google.inject.Inject>>() {
				}).in(Singleton.class);
			}
		});

		return true;
	}
}
