package com.nhl.bootique.jersey;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import com.google.inject.Injector;

public class GuiceBridgeFeature implements Feature {

	static final String INJECTOR_PROPERTY = "com.nhl.bootique.jersey.injector";

	static Injector getInjector(Configuration configuration) {
		Injector injector = (Injector) configuration.getProperty(GuiceBridgeFeature.INJECTOR_PROPERTY);
		if (injector == null) {
			throw new IllegalStateException("Injector is not available in JAX RS runtime. Use property '"
					+ GuiceBridgeFeature.INJECTOR_PROPERTY + "' to set it");
		}

		return injector;
	}

	private ServiceLocator hk2Container;

	@Inject
	public GuiceBridgeFeature(ServiceLocator hk2Container) {
		this.hk2Container = hk2Container;
	}

	@Override
	public boolean configure(FeatureContext context) {
		GuiceBridge.getGuiceBridge().initializeGuiceBridge(hk2Container);

		GuiceIntoHK2Bridge guiceBridge = hk2Container.getService(GuiceIntoHK2Bridge.class);

		Injector injector = getInjector(context.getConfiguration());

		guiceBridge.bridgeGuiceInjector(injector);

		// also allow Guice Inject flavor
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
