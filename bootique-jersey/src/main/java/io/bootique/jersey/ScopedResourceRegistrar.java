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

import io.bootique.di.Injector;
import io.bootique.di.Key;
import io.bootique.jetty.MappedServlet;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.inject.hk2.AbstractBinder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.Objects;

/**
 * Registers itself with Jersey HK2 as a custom supplier of a given API resource class. This is needed to enforce
 * resource instance scope (singleton vs. per-request) alignment between Jersey and Bootique.
 *
 * @since 4.0
 */
class ScopedResourceRegistrar<T> implements ResourceRegistrar<T> {

    private final Class<T> resourceType;
    private final Key<T> resourceKey;

    @Inject
    private Injector injector;

    @Inject
    private Provider<MappedServlet<ServletContainer>> jerseyServlet;

    public ScopedResourceRegistrar(Class<T> resourceType) {
        this.resourceType = resourceType;
        this.resourceKey = Key.get(resourceType);
    }

    @Override
    public void registerResource(FeatureContext context) {
        if (isSingleton()) {
            context.register(injector.getInstance(resourceKey));
        } else {
            context.register(resourceType);
        }
    }

    @Override
    public void registerResourceSupplier(AbstractBinder binder) {
        if (!isSingleton()) {
            binder.bindFactory(this::getPerRequest).to(resourceType);
        }
    }

    private T getPerRequest() {
        Objects.requireNonNull(jerseyServlet, "'jerseyServlet' was not injected");

        T t = injector.getInstance(resourceKey);
        jerseyServlet.get().getServlet().getApplicationHandler().getInjectionManager().inject(t);
        return t;
    }

    private boolean isSingleton() {
        // a hack: triggering instance creation for implicit registrations to avoid an exception in "isSingleton"
        // side effect is that for non-singleton endpoints we'll create a throwaway instance
        if (!injector.hasProvider(resourceKey)) {
            injector.getInstance(resourceKey);
        }

        return injector.isSingleton(resourceKey);
    }
}
