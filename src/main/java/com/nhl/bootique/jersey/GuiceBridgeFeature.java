package com.nhl.bootique.jersey;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import com.google.inject.Injector;

public class GuiceBridgeFeature implements Feature {

	static final String INJECTOR_PROPERTY = "com.nhl.bootique.jersey.injector";

	private ServiceLocator hk2Container;

	@Inject
	public GuiceBridgeFeature(ServiceLocator hk2Container) {
		this.hk2Container = hk2Container;
	}

	@Override
	public boolean configure(FeatureContext context) {
		GuiceBridge.getGuiceBridge().initializeGuiceBridge(hk2Container);

		GuiceIntoHK2Bridge guiceBridge = hk2Container.getService(GuiceIntoHK2Bridge.class);

		Injector injector = (Injector) context.getConfiguration().getProperty(INJECTOR_PROPERTY);
		if (injector == null) {
			throw new IllegalStateException(
					"Injector is not available in JAX RS runtime. Use property '" + INJECTOR_PROPERTY + "' to set it");
		}

		guiceBridge.bridgeGuiceInjector(injector);

		return true;
	}
}
