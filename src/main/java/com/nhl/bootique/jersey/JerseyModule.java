package com.nhl.bootique.jersey;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;

import org.glassfish.jersey.server.ResourceConfig;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.ConfigModule;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.jetty.JettyModule;
import com.nhl.bootique.jetty.MappedServlet;

public class JerseyModule extends ConfigModule {

	private String urlPattern = "/*";
	private Class<? extends Application> application;
	private Collection<Class<?>> resources = new HashSet<>();
	private Collection<String> packageRoots = new HashSet<>();

	/**
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.11
	 * @return returns a {@link Multibinder} for JAX-RS Features.
	 */
	public static Multibinder<Feature> contributeFeatures(Binder binder) {
		return Multibinder.newSetBinder(binder, Feature.class);
	}

	/**
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.12
	 * @return returns a {@link Multibinder} for JAX-RS DynamicFeatures.
	 */
	public static Multibinder<DynamicFeature> contributeDynamicFeatures(Binder binder) {
		return Multibinder.newSetBinder(binder, DynamicFeature.class);
	}

	/**
	 * Creates a builder of {@link JerseyModule}.
	 * 
	 * @since 0.11
	 */
	public static Builder builder() {
		return new Builder();
	}

	protected JerseyModule() {
		// non-public - module must be created via Builder
	}

	@Override
	public void configure(Binder binder) {

		JettyModule.contributeServlets(binder).addBinding().to(Key.get(MappedServlet.class, JerseyServlet.class));

		// trigger extension points creation and provide default contributions
		JerseyModule.contributeFeatures(binder);
		JerseyModule.contributeDynamicFeatures(binder);
	}

	@Singleton
	@Provides
	private ResourceConfig createResourceConfig(Injector injector, Set<Feature> features,
			Set<DynamicFeature> dynamicFeatures) {
		ResourceConfig config = application != null ? ResourceConfig.forApplicationClass(application)
				: new ResourceConfig();

		packageRoots.forEach(p -> config.packages(true, p));
		resources.forEach(r -> config.register(r));
		features.forEach(f -> config.register(f));
		dynamicFeatures.forEach(df -> config.register(df));

		// TODO: make this pluggable?
		config.register(ResourceModelDebugger.class);

		// register Guice Injector as a service in Jersey HK2, and
		// GuiceBridgeFeature as a
		GuiceBridgeFeature.register(config, injector);

		return config;
	}

	@JerseyServlet
	@Provides
	@Singleton
	private MappedServlet createJerseyServlet(ConfigurationFactory configFactory, ResourceConfig config) {
		return configFactory.config(JerseyServletFactory.class, configPrefix).initUrlPatternIfNotSet(urlPattern)
				.createJerseyServlet(config);
	}

	public static class Builder {

		private JerseyModule module;

		private Builder() {
			this.module = new JerseyModule();
		}

		public JerseyModule build() {
			return module;
		}

		/**
		 * @since 0.11
		 * @param urlPattern
		 *            a URL pattern for the Jersey servlet within Jetty app
		 *            context.
		 * @return self
		 */
		public Builder urlPattern(String urlPattern) {
			module.urlPattern = urlPattern;
			return this;
		}

		public Builder resource(Class<?> resourceType) {
			module.resources.add(resourceType);
			return this;
		}

		public Builder packageRoot(Package aPackage) {
			module.packageRoots.add(aPackage.getName());
			return this;
		}

		public Builder packageRoot(Class<?> classFromPackage) {
			// TODO: test with inner classes
			String name = classFromPackage.getName();
			int dot = name.lastIndexOf('.');
			if (dot <= 0) {
				throw new IllegalArgumentException("Class is in default package - unsupported");
			}

			module.packageRoots.add(name.substring(0, dot));
			return this;
		}

		public <T extends Application> Builder application(Class<T> application) {
			module.application = application;
			return this;
		}

	}
}
