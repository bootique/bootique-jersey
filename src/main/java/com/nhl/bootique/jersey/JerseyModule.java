package com.nhl.bootique.jersey;

import java.util.Collection;
import java.util.HashSet;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.nhl.bootique.jetty.JettyModule;

// TODO: should we turn this into factorymodule? we'll be able to start Jersey from YAML then
public class JerseyModule implements Module {

	private Class<? extends Application> application;
	private Collection<Class<?>> resources = new HashSet<>();
	private Collection<String> packageRoots = new HashSet<>();

	public <T extends Application> JerseyModule application(Class<T> application) {
		this.application = application;
		return this;
	}

	public JerseyModule resource(Class<?> resourceType) {
		resources.add(resourceType);
		return this;
	}

	public JerseyModule packageRoot(Package aPackage) {
		packageRoots.add(aPackage.getName());
		return this;
	}

	public JerseyModule packageRoot(Class<?> classFromPackage) {
		String name = classFromPackage.getName();
		int dot = name.lastIndexOf('.');
		if (dot <= 0) {
			throw new IllegalArgumentException("Class is in default package - unsupported");
		}

		packageRoots.add(name.substring(0, dot));
		return this;
	}

	@Override
	public void configure(Binder binder) {
		// TODO: map servlet path as a YAML property
		JettyModule.servletBinder(binder).addBinding("/*").to(ServletContainer.class);
	}

	@Singleton
	@Provides
	private ServletContainer createJerseyServlet(Injector injector) {
		ResourceConfig config = application != null ? ResourceConfig.forApplicationClass(application)
				: new ResourceConfig();

		packageRoots.forEach(p -> config.packages(true, p));
		resources.forEach(r -> config.register(r));

		GuiceBridgeFeature.register(config, injector);
		return new ServletContainer(config);
	}
}
