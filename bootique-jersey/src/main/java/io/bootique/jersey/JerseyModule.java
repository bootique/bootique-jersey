package io.bootique.jersey;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import java.util.Set;

public class JerseyModule extends ConfigModule {

    private static final String URL_PATTERN = "/*";

    /**
     * Returns an instance of {@link JerseyModuleExtender} used by downstream modules to load custom extensions of
     * services declared in the JerseyModule. Should be invoked from a downstream Module's "configure" method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link JerseyModuleExtender} that can be used to load Jersey custom extensions.
     * @since 0.21
     */
    public static JerseyModuleExtender extend(Binder binder) {
        return new JerseyModuleExtender(binder);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for JAX-RS Features.
     * @since 0.11
     * @deprecated since 0.21 call {@link #extend(Binder)} and then call
     * {@link JerseyModuleExtender#addFeature(Feature)}.
     */
    @Deprecated
    public static Multibinder<Feature> contributeFeatures(Binder binder) {
        return Multibinder.newSetBinder(binder, Feature.class);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for JAX-RS DynamicFeatures.
     * @since 0.12
     * @deprecated since 0.21 call {@link #extend(Binder)} and then call
     * {@link JerseyModuleExtender#addDynamicFeature(DynamicFeature)}.
     */
    @Deprecated
    public static Multibinder<DynamicFeature> contributeDynamicFeatures(Binder binder) {
        return Multibinder.newSetBinder(binder, DynamicFeature.class);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for explicitly registered JAX-RS
     * resource types.
     * @since 0.15
     * @deprecated since 0.21 call {@link #extend(Binder)} and then call
     * {@link JerseyModuleExtender#addResource(Class)} or similar method.
     */
    @Deprecated
    public static Multibinder<Object> contributeResources(Binder binder) {
        return Multibinder.newSetBinder(binder, Key.get(Object.class, JerseyResource.class));
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for packages that contain JAX-RS
     * resource classes.
     * @since 0.15
     * @deprecated since 0.21 call {@link #extend(Binder)} and then call
     * {@link JerseyModuleExtender#addPackage(Package)} .
     */
    @Deprecated
    public static Multibinder<Package> contributePackages(Binder binder) {
        return Multibinder.newSetBinder(binder, Package.class);
    }

    @Override
    public void configure(Binder binder) {

        JettyModule.extend(binder).addMappedServlet(new TypeLiteral<MappedServlet<ServletContainer>>() {
        });

        JerseyModule.extend(binder).initAllExtensions();
    }

    @Singleton
    @Provides
    private ResourceConfig createResourceConfig(Injector injector, Set<Feature> features,
                                                Set<DynamicFeature> dynamicFeatures, @JerseyResource Set<Object> resources, Set<Package> packages) {

        ResourceConfig config = new ResourceConfig();

        packages.forEach(p -> config.packages(true, p.getName()));
        resources.forEach(r -> config.register(r));

        features.forEach(f -> config.register(f));
        dynamicFeatures.forEach(df -> config.register(df));

        // TODO: make this pluggable?
        config.register(ResourceModelDebugger.class);

        // register Guice Injector as a service in Jersey HK2, and GuiceBridgeFeature as a
        GuiceBridgeFeature.register(config, injector);

        return config;
    }

    @Provides
    @Singleton
    private MappedServlet<ServletContainer> provideJerseyServlet(ConfigurationFactory configFactory, ResourceConfig config) {
        return configFactory
                .config(JerseyServletFactory.class, configPrefix)
                .initUrlPatternIfNotSet(URL_PATTERN)
                .createJerseyServlet(config);
    }
}
