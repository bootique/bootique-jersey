/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jersey;

import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.BQInject;
import io.bootique.di.Binder;
import io.bootique.di.Injector;
import io.bootique.di.Provides;
import io.bootique.di.TypeLiteral;
import io.bootique.jersey.jaxrs.LocalDateConverter;
import io.bootique.jersey.jaxrs.LocalDateTimeConverter;
import io.bootique.jersey.jaxrs.LocalTimeConverter;
import io.bootique.jersey.jaxrs.MappedParamConvertersProvider;
import io.bootique.jersey.jaxrs.YearConverter;
import io.bootique.jersey.jaxrs.YearMonthConverter;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.JustInTimeInjectionResolver;
import org.glassfish.jersey.inject.hk2.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JerseyModule implements BQModule {

    private static final String CONFIG_PREFIX = "jersey";
    static final String DISABLE_WADL_PROPERTY = "jersey.config.server.wadl.disableWadl";

    static final String PROPERTIES_BINDING = "io.bootique.jersey.properties";
    static final String RESOURCE_PACKAGES_BINDING = "io.bootique.jersey.resourcePackages";
    static final String RESOURCES_PATH_OVERRIDE_BINDING = "io.bootique.jersey.resourcePathOverrides";
    static final String PROVIDERS_BINDING = "io.bootique.jersey.providers";
    static final String LEGACY_RESOURCES_BINDING = "io.bootique.jersey.legacyResources";

    /**
     * Returns an instance of {@link JerseyModuleExtender} used by downstream modules to load custom extensions of
     * services declared in the JerseyModule. Should be invoked from a downstream Module's "configure" method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link JerseyModuleExtender} that can be used to load Jersey custom extensions.
     */
    public static JerseyModuleExtender extend(Binder binder) {
        return new JerseyModuleExtender(binder);
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Integrates Jersey JAX-RS HTTP server")
                .config(CONFIG_PREFIX, JerseyServletFactory.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {

        JettyModule.extend(binder).addMappedServlet(new TypeLiteral<MappedServlet<ServletContainer>>() {
        });

        JerseyModule.extend(binder).initAllExtensions();
    }

    @Singleton
    @Provides
    ResourceConfig createResourceConfig(
            Injector injector,
            Set<Feature> features,
            Set<DynamicFeature> dynamicFeatures,
            @Named(PROVIDERS_BINDING) Set<Object> providers,
            Set<ResourceRegistrar<?>> resourceRegistrars,
            @Named(RESOURCE_PACKAGES_BINDING) Set<Package> packages,
            @Named(RESOURCES_PATH_OVERRIDE_BINDING) Map<String, Class<?>> resourcePathOverrides,
            Set<MappedResource<?>> mappedResources,
            Map<Class<?>, ParamConverter<?>> paramConverters,
            @Named(PROPERTIES_BINDING) Map<String, Object> properties,

            // these two are deprecated
            @Named(LEGACY_RESOURCES_BINDING) Set<Object> legacyResources,
            @Named(RESOURCES_PATH_OVERRIDE_BINDING) Map<String, Object> legacyResourcesByPath) {

        ResourceConfig config = createResourceConfig(injector);

        // configure a bridge between BQ DI and Jersey HK2
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(injector).to(Injector.class).in(Singleton.class);
                bind(BqInjectorBridge.class).to(JustInTimeInjectionResolver.class).in(Singleton.class);
                bind(BqInjectInjector.class).to(new GenericType<InjectionResolver<BQInject>>() {
                }).in(Singleton.class);
            }
        });

        packages.forEach(p -> config.packages(true, p.getName()));

        // wrap registration of resources in a Feature. Otherwise, Jersey prints a warning because resources are registered
        // as instances instead of classes - https://github.com/eclipse-ee4j/jersey/issues/3700

        config.register((Feature) context -> {

            providers.forEach(context::register);

            // TODO: this is deprecated since 4.0, as it does not respect the declared scope of the resources
            legacyResources.forEach(context::register);

            resourceRegistrars.forEach(rs -> rs.registerResource(context));

            if (!mappedResources.isEmpty() || !resourcePathOverrides.isEmpty() || !legacyResourcesByPath.isEmpty()) {
                // first register under the @Path from annotation, then override it via ResourcePathCustomizer
                mappedResources.forEach(mr -> context.register(mr.getResource()));
                legacyResourcesByPath.values().forEach(context::register);

                Map<String, Class<?>> pathOverrides = new HashMap<>(resourcePathOverrides);
                legacyResourcesByPath.forEach((k, v) -> pathOverrides.put(k, v.getClass()));

                context.register(ResourcePathCustomizer.create(mappedResources, pathOverrides));
            }

            return true;
        });

        // This level of indirection is needed to preserve DI-declared scope
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                resourceRegistrars.forEach(rs -> rs.registerResourceSupplier(this));
            }
        });

        ParamConverterProvider converterProvider = createParamConvertersProvider(paramConverters);
        config.register(converterProvider);

        features.forEach(config::register);
        dynamicFeatures.forEach(config::register);

        config.addProperties(properties);

        // disable WADL by default, unless explicitly enabled
        if (!properties.containsKey(DISABLE_WADL_PROPERTY)) {
            config.property(DISABLE_WADL_PROPERTY, true);
        }

        config.register(ResourceModelDebugger.class);

        return config;
    }

    protected ParamConverterProvider createParamConvertersProvider(Map<Class<?>, ParamConverter<?>> paramConverters) {

        // start with standard converters, and allow customer overrides of those
        Map<Class<?>, ParamConverter<?>> allConverters = new HashMap<>();
        allConverters.put(LocalDate.class, new LocalDateConverter());
        allConverters.put(LocalTime.class, new LocalTimeConverter());
        allConverters.put(LocalDateTime.class, new LocalDateTimeConverter());
        allConverters.put(Year.class, new YearConverter());
        allConverters.put(YearMonth.class, new YearMonthConverter());

        allConverters.putAll(paramConverters);
        return new MappedParamConvertersProvider(allConverters);
    }

    protected ResourceConfig createResourceConfig(Injector injector) {
        // application is an optional binding. If not defined, build a default ResourceConfig
        return injector.hasProvider(Application.class)
                ? ResourceConfig.forApplication(injector.getInstance(Application.class))
                : new ResourceConfig();
    }

    @Provides
    @Singleton
    private MappedServlet<ServletContainer> provideJerseyServlet(ConfigurationFactory configFactory) {
        return configFactory.config(JerseyServletFactory.class, CONFIG_PREFIX).createJerseyServlet();
    }
}
