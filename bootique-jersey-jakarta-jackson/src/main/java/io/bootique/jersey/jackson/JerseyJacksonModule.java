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
package io.bootique.jersey.jackson;

import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jersey.JerseyModule;

import jakarta.inject.Singleton;

/**
 * @since 2.0
 */
public class JerseyJacksonModule implements BQModule {

    private static final String CONFIG_PREFIX = "jerseyjackson";

    public static JerseyJacksonModuleExtender extend(Binder binder) {
        return new JerseyJacksonModuleExtender(binder);
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Integrates Jackson JSON serializers to Jersey JAX-RS engine")
                .config(CONFIG_PREFIX, JerseyJacksonFactory.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {
        JerseyModule.extend(binder).addFeature(ObjectMapperResolverFeature.class);
        JerseyJacksonModule.extend(binder).initAllExtensions();
    }

    @Singleton
    @Provides
    ObjectMapperResolverFeature provideObjectMapperResolverFeature(ConfigurationFactory configFactory) {

        ObjectMapperResolver omr = configFactory
                .config(JerseyJacksonFactory.class, CONFIG_PREFIX)
                .createObjectMapperResolver();

        return new ObjectMapperResolverFeature(omr);
    }
}
