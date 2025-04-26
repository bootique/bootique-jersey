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
import io.bootique.di.*;
import io.bootique.jersey.jaxrs.*;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;
import jakarta.inject.Named;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.JustInTimeInjectionResolver;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import jakarta.inject.Singleton;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JerseyModule implements BQModule {

    private static final String CONFIG_PREFIX = "jersey";
    static final String RESOURCES_BY_PATH_BINDING = "io.bootique.jersey.jakarta.resourcesByPath";

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
    private ResourceConfig createResourceConfig(
            Injector injector,
            Set<Feature> features,
            Set<DynamicFeature> dynamicFeatures,
            @JerseyResource Set<Object> resources,
            @JerseyResource Set<Package> packages,
            @Named(RESOURCES_BY_PATH_BINDING) Map<String, Object> resourcesByPath,
            Set<MappedResource> mappedResources,
            Map<Class<?>, ParamConverter> paramConverters,
            @JerseyResource Map<String, Object> properties) {

        ResourceConfig config = createResourceConfig(injector);

        // configure bridge between BQ DI and Jersey HK2
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(injector).to(Injector.class).in(jakarta.inject.Singleton.class);
                bind(BqInjectorBridge.class).to(JustInTimeInjectionResolver.class).in(jakarta.inject.Singleton.class);
                bind(JavaxInjectInjector.class).to(new GenericType<InjectionResolver<javax.inject.Inject>>() {}).in(jakarta.inject.Singleton.class);
                bind(BqInjectInjector.class).to(new GenericType<InjectionResolver<BQInject>>() {}).in(jakarta.inject.Singleton.class);
            }
        });

        packages.forEach(p -> config.packages(true, p.getName()));

        // wrap registration of resources in a Feature. Otherwise, Jersey prints a warning because resources are registered
        // as instances instead of classes - https://github.com/eclipse-ee4j/jersey/issues/3700

        config.register((Feature) context -> {

            resources.forEach(context::register);

            if (!mappedResources.isEmpty() || !resourcesByPath.isEmpty()) {
                // first register under the @Path from annotation, then override it via ResourcePathCustomizer
                mappedResources.forEach(mr -> context.register(mr.getResource()));
                resourcesByPath.values().forEach(context::register);

                context.register(ResourcePathCustomizer.create(mappedResources, resourcesByPath));
            }

            return true;
        });

        ParamConverterProvider converterProvider = createParamConvertersProvider(paramConverters);
        config.register(converterProvider);

        features.forEach(config::register);
        dynamicFeatures.forEach(config::register);

        config.addProperties(properties);

        // TODO: make this pluggable?
        config.register(ResourceModelDebugger.class);

        return config;
    }

    protected ParamConverterProvider createParamConvertersProvider(Map<Class<?>, ParamConverter> paramConverters) {

        // start with standard converters, and allow customer overrides of those
        Map<Class<?>, ParamConverter> allConverters = new HashMap<>();
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
