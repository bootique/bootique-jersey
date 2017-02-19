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
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for JAX-RS Features.
     * @since 0.11
     */
    public static Multibinder<Feature> contributeFeatures(Binder binder) {
        return Multibinder.newSetBinder(binder, Feature.class);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for JAX-RS DynamicFeatures.
     * @since 0.12
     */
    public static Multibinder<DynamicFeature> contributeDynamicFeatures(Binder binder) {
        return Multibinder.newSetBinder(binder, DynamicFeature.class);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for explicitly registered JAX-RS
     * resource types.
     * @since 0.15
     */
    public static Multibinder<Object> contributeResources(Binder binder) {
        return Multibinder.newSetBinder(binder, Key.get(Object.class, JerseyResource.class));
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for packages that contain JAX-RS
     * resource classes.
     * @since 0.15
     */
    public static Multibinder<Package> contributePackages(Binder binder) {
        return Multibinder.newSetBinder(binder, Package.class);
    }

    @Override
    public void configure(Binder binder) {

        JettyModule.extend(binder).addMappedServlet(new TypeLiteral<MappedServlet<ServletContainer>>() {
        });

        // trigger extension points creation and provide default contributions
        JerseyModule.contributeFeatures(binder);
        JerseyModule.contributeDynamicFeatures(binder);
        JerseyModule.contributePackages(binder);
        JerseyModule.contributeResources(binder);
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

        // register Guice Injector as a service in Jersey HK2, and
        // GuiceBridgeFeature as a
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
