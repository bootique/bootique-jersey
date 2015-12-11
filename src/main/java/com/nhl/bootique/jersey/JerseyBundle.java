package com.nhl.bootique.jersey;

import java.util.Collection;
import java.util.HashSet;

import javax.servlet.Servlet;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.nhl.bootique.jetty.JettyBundle;

public class JerseyBundle {

	private Class<? extends Application> application;
	private Collection<Class<?>> resources;
	private Collection<String> packageRoots;

	public static JerseyBundle create() {
		return new JerseyBundle();
	}

	private JerseyBundle() {
		this.resources = new HashSet<>();
		this.packageRoots = new HashSet<>();
	}

	public <T extends Application> JerseyBundle application(Class<T> application) {
		this.application = application;
		return this;
	}

	public JerseyBundle resource(Class<?> resourceType) {
		resources.add(resourceType);
		return this;
	}

	public JerseyBundle packageRoot(Package aPackage) {
		packageRoots.add(aPackage.getName());
		return this;
	}

	public JerseyBundle packageRoot(Class<?> classFromPackage) {
		String name = classFromPackage.getName();
		int dot = name.lastIndexOf('.');
		if (dot <= 0) {
			throw new IllegalArgumentException("Class is in default package - unsupported");
		}

		packageRoots.add(name.substring(0, dot));
		return this;
	}

	public Module module() {
		return new JerseyModule();
	}

	class JerseyModule implements Module {

		@Override
		public void configure(Binder binder) {
			// TODO: map servlet path as a YAML property
			JettyBundle.servletBinder(binder).addBinding("/*").toInstance(createJerseyServlet());
		}

		private Servlet createJerseyServlet() {
			ResourceConfig config = application != null ? ResourceConfig.forApplicationClass(application)
					: new ResourceConfig();

			packageRoots.forEach(p -> config.packages(true, p));
			resources.forEach(r -> config.register(r));
			return new ServletContainer(config);
		}
	}
}
