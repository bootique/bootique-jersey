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

package io.bootique.jersey.client;

import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Injector;
import io.bootique.di.Provides;

import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import java.util.Set;

/**
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class JerseyClientModule implements BQModule {

    private static final String CONFIG_PREFIX = "jerseyclient";

    /**
     * Returns an instance of {@link JerseyClientModuleExtender} used by downstream modules to load custom extensions of
     * services declared in the JerseyClientModule. Should be invoked from a downstream Module's "configure" method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link JerseyClientModuleExtender} that can be used to load JerseyClientModule extensions.
     */
    public static JerseyClientModuleExtender extend(Binder binder) {
        return new JerseyClientModuleExtender(binder);
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(new JerseyClientModule())
                .description("Deprecated, can be replaced with 'bootique-jersey-jakarta-client'.")
                .config(CONFIG_PREFIX, HttpClientFactoryFactory.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {
        extend(binder).initAllExtensions();
    }

    @Provides
    @Singleton
    HttpClientFactoryFactory provideClientFactoryFactory(ConfigurationFactory configFactory) {
        return configFactory.config(HttpClientFactoryFactory.class, CONFIG_PREFIX);
    }

    @Provides
    @Singleton
    HttpClientFactory provideClientFactory(
            HttpClientFactoryFactory factoryFactory,
            Injector injector,
            @JerseyClientFeatures Set<Feature> features) {
        
        return factoryFactory.createClientFactory(injector, features);
    }

    @Provides
    @Singleton
    HttpTargets provideTargets(HttpClientFactoryFactory factoryFactory, HttpClientFactory clientFactory) {
        return factoryFactory.createTargets(clientFactory);
    }
}
