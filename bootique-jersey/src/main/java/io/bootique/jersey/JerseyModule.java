package io.bootique.jersey;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import java.util.Map;
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

    @Override
    public void configure(Binder binder) {

        JettyModule.extend(binder).addMappedServlet(new TypeLiteral<MappedServlet<ServletContainer>>() {
        });

        JerseyModule.extend(binder).initAllExtensions();
    }

    @Singleton
    @Provides
    private ResourceConfig createResourceConfig(Injector injector,
                                                Set<Feature> features,
                                                Set<DynamicFeature> dynamicFeatures,
                                                @JerseyResource Set<Object> resources, Set<Package> packages,
                                                @JerseyResource Map<String, Object> properties) {

        ResourceConfig config = new ResourceConfig();

        packages.forEach(p -> config.packages(true, p.getName()));
        resources.forEach(r -> config.register(r));

        features.forEach(f -> config.register(f));
        dynamicFeatures.forEach(df -> config.register(df));

        config.addProperties(properties);

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
