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

import com.google.inject.*;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.GenericType;
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
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                GuiceInjectInjector guiceInjector = new GuiceInjectInjector(injector);

                bind(guiceInjector)
                        .to(new GenericType<InjectionResolver<Inject>>(){})
                        .in(javax.inject.Singleton.class);
            }
        });

        packages.forEach(p -> config.packages(true, p.getName()));
        resources.forEach(r -> config.register(r));

        features.forEach(f -> config.register(f));
        dynamicFeatures.forEach(df -> config.register(df));

        config.addProperties(properties);

        // TODO: make this pluggable?
        config.register(ResourceModelDebugger.class);

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
