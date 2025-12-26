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

import io.bootique.ModuleExtender;
import io.bootique.di.Binder;
import io.bootique.di.Key;
import io.bootique.di.MapBuilder;
import io.bootique.di.SetBuilder;
import io.bootique.di.TypeLiteral;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.ext.ParamConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JerseyModuleExtender extends ModuleExtender<JerseyModuleExtender> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JerseyModuleExtender.class);

    private SetBuilder<Feature> features;
    private SetBuilder<DynamicFeature> dynamicFeatures;
    private SetBuilder<Object> providers;
    private SetBuilder<ResourceRegistrar<?>> resourceRegistrars;
    private SetBuilder<Package> packages;
    private SetBuilder<MappedResource<?>> mappedResources;
    private MapBuilder<String, Class<?>> resourcePathOverrides;
    private MapBuilder<String, Object> properties;
    private MapBuilder<Class<?>, ParamConverter<?>> paramConverters;


    @Deprecated
    private SetBuilder<Object> legacyResources;

    @Deprecated
    private MapBuilder<String, Object> legacyResourcesByPath;

    JerseyModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public JerseyModuleExtender initAllExtensions() {
        contributeDynamicFeatures();
        contributeFeatures();
        contributePackages();
        contributeProviders();
        contributeResourceRegistrars();
        contributeResourcePathOverrides();
        contributeMappedResources();
        contributeProperties();
        contributeParamConverters();

        contributeLegacyResources();
        contributeLegacyResourcesByPath();

        return this;
    }

    public JerseyModuleExtender addPackage(Package aPackage) {
        contributePackages().addInstance(aPackage);
        return this;
    }

    public JerseyModuleExtender addPackage(Class<?> anyClassInPackage) {
        contributePackages().addInstance(anyClassInPackage.getPackage());
        return this;
    }

    public <T> JerseyModuleExtender addParamConverter(Class<T> valueType, Class<? extends ParamConverter<T>> converterType) {
        contributeParamConverters().put(valueType, converterType);
        return this;
    }

    public <T> JerseyModuleExtender addParamConverter(Class<T> valueType, ParamConverter<T> converterType) {
        contributeParamConverters().putInstance(valueType, converterType);
        return this;
    }

    public JerseyModuleExtender addFeature(Feature feature) {
        contributeFeatures().addInstance(feature);
        return this;
    }

    public <T extends Feature> JerseyModuleExtender addFeature(Class<T> featureType) {
        contributeFeatures().add(featureType);
        return this;
    }

    public JerseyModuleExtender addDynamicFeature(DynamicFeature feature) {
        contributeDynamicFeatures().addInstance(feature);
        return this;
    }

    public <T extends DynamicFeature> JerseyModuleExtender addDynamicFeature(Class<T> featureType) {
        contributeDynamicFeatures().add(featureType);
        return this;
    }

    /**
     * @deprecated This method doesn't work correctly with per-request resource scope. You must use either
     * {@link #addApiResource(Object)} or {@link #addProvider(Object)} depending on what you are registering.
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public JerseyModuleExtender addResource(Object resource) {
        LOGGER.warn("** Using deprecated 'addResource'");
        contributeLegacyResources().addInstance(resource);
        return this;
    }

    /**
     * @deprecated This method doesn't work correctly with per-request resource scope. You must use
     * {@link #addApiResource(Class)} (or, sometimes, {@link #addProvider(Class)}, if you used this method to register
     * arbitrary JAX-RS providers). In case of an API resource, consider whether it (or its Bootique provider method)
     * should now to be explicitly annotated with {@link jakarta.inject.Singleton}, as per-request resources incur more
     * overhead.
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public JerseyModuleExtender addResource(Class<?> resourceType) {
        LOGGER.warn("** Using deprecated 'addResource'. This may incorrectly force a singleton scope on per-request resources.");
        contributeLegacyResources().add(resourceType);
        return this;
    }


    /**
     * @since 2.0
     * @deprecated This method doesn't work correctly with per-request resource scope. You must use
     * {@link #addApiResource(Class, String)}. When doing this, consider whether it (or its Bootique provider method)
     * should now to be explicitly annotated with {@link jakarta.inject.Singleton}, as per-request resources incur more
     * overhead.
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public JerseyModuleExtender addResource(Class<?> resource, String path) {
        contributeLegacyResourcesByPath().put(path, resource);
        return this;
    }

    /**
     * Adds a JAX-RS provider instance, such as message body reader, writer, exception mapper, etc.
     *
     * @since 4.0
     */
    public JerseyModuleExtender addProvider(Object provider) {
        contributeProviders().addInstance(provider);
        return this;
    }

    /**
     * Adds a JAX-RS provider, such as message body reader, writer, exception mapper, etc.
     *
     * @since 4.0
     */
    public JerseyModuleExtender addProvider(Class<?> providerType) {
        contributeProviders().add(providerType);
        return this;
    }

    /**
     * Registers a resource instance with Jersey. Resource instance scope will be singleton (it will be reused across
     * requests). Jersey will provide injection for the <code>@Context</code> annotation.
     *
     * @since 4.0
     */
    public JerseyModuleExtender addApiResource(Object resource) {
        contributeResourceRegistrars().addInstance(new SingletonResourceRegistrar<>(resource));
        return this;
    }

    /**
     * Registers an API resource type with Jersey. Resource instance scope (singleton vs per-request) will be aligned
     * with the Bootique scope for this type. Jersey will provide injection for the <code>@Context</code> annotation.
     *
     * @since 4.0
     */
    public JerseyModuleExtender addApiResource(Class<?> resourceType) {
        contributeResourceRegistrars().addInstance(new ScopedResourceRegistrar<>(resourceType));
        return this;
    }

    /**
     * Registers an API resource type with a custom path. The path argument overrides class-level
     * {@link jakarta.ws.rs.Path} annotation value, allowing to remap resource to a different URL. This method also
     * allows to map the same resource multiple times under different paths.
     *
     * @param resource type of the resource
     * @param path     resource URL path that overrides {@link jakarta.ws.rs.Path} annotation on the resource class.
     * @since 4.0
     */
    public JerseyModuleExtender addApiResource(Class<?> resource, String path) {
        addApiResource(resource);
        contributeResourcePathOverrides().putInstance(path, resource);
        return this;
    }

    /**
     * Registers a "mapped" API resource. MappedResource paths override class-level {@link jakarta.ws.rs.Path} annotation
     * value, allowing to remap resource to a different URL. This even allows to map the same resource multiple times
     * under different paths.
     *
     * @param mappedResource an object that encapsulates resource object and one or more alternative path mappings.
     * @return this extender
     * @since 2.0
     */
    public <T> JerseyModuleExtender addMappedResource(MappedResource<T> mappedResource) {
        contributeMappedResources().addInstance(mappedResource);
        return this;
    }

    /**
     * Registers a "mapped" API resource. MappedResource paths override class-level {@link jakarta.ws.rs.Path} annotation
     * value, allowing to remap resource to a different URL. This even allows to map the same resource multiple times
     * under different paths.
     *
     * @param mappedResourceKey a DI key of a {@link MappedResource}
     * @return this extender
     * @since 2.0
     */
    public <T> JerseyModuleExtender addMappedResource(Key<MappedResource<T>> mappedResourceKey) {
        contributeMappedResources().add(mappedResourceKey);
        return this;
    }

    /**
     * Registers a "mapped" API resource. MappedResource paths override class-level {@link jakarta.ws.rs.Path} annotation
     * value, allowing to remap resource to a different URL. This even allows to map the same resource multiple times
     * under different paths.
     *
     * @param mappedResourceType a signature of the {@link MappedResource}. The resource itself is located in DI.
     * @return this extender
     * @since 2.0
     */
    public <T> JerseyModuleExtender addMappedResource(TypeLiteral<MappedResource<T>> mappedResourceType) {
        contributeMappedResources().add(Key.get(mappedResourceType));
        return this;
    }

    /**
     * @since 2.0
     */
    public JerseyModuleExtender setApplication(Application app) {
        binder.bind(Application.class).toInstance(app);
        return this;
    }

    /**
     * @since 2.0
     */
    public JerseyModuleExtender setApplication(Class<? extends Application> appType) {
        binder.bind(Application.class).to(appType);
        return this;
    }

    /**
     * Sets Jersey container property. This allows setting ResourceConfig properties that can not be set via JAX RS features.
     *
     * @param name  property name
     * @param value property value
     * @return this extender
     * @see org.glassfish.jersey.server.ServerProperties
     */
    public JerseyModuleExtender setProperty(String name, Object value) {
        contributeProperties().putInstance(name, value);
        return this;
    }

    /**
     * Sets Jersey container properties.  This allows setting ResourceConfig properties that can not be set via JAX RS features.
     *
     * @return this extender instance
     * @see org.glassfish.jersey.server.ServerProperties
     */
    public JerseyModuleExtender setProperties(Map<String, String> properties) {
        properties.forEach(this::setProperty);
        return this;
    }

    MapBuilder<String, Class<?>> contributeResourcePathOverrides() {
        if (resourcePathOverrides == null) {
            resourcePathOverrides = newMap(
                    new TypeLiteral<>() {
                    },
                    new TypeLiteral<>() {
                    },
                    JerseyModule.RESOURCES_PATH_OVERRIDE_BINDING);
        }
        return resourcePathOverrides;
    }


    SetBuilder<MappedResource<?>> contributeMappedResources() {
        if (mappedResources == null) {
            mappedResources = newSet(new TypeLiteral<>() {
            });
        }
        return mappedResources;
    }

    MapBuilder<String, Object> contributeProperties() {
        if (properties == null) {
            properties = newMap(String.class, Object.class, JerseyModule.PROPERTIES_BINDING);
        }
        return properties;
    }

    SetBuilder<Feature> contributeFeatures() {
        if (features == null) {
            features = newSet(Feature.class);
        }
        return features;
    }

    SetBuilder<DynamicFeature> contributeDynamicFeatures() {
        if (dynamicFeatures == null) {
            dynamicFeatures = newSet(DynamicFeature.class);
        }
        return dynamicFeatures;
    }

    SetBuilder<Object> contributeProviders() {
        if (providers == null) {
            providers = newSet(Key.get(Object.class, JerseyModule.PROVIDERS_BINDING));
        }
        return providers;
    }

    SetBuilder<ResourceRegistrar<?>> contributeResourceRegistrars() {
        if (resourceRegistrars == null) {
            resourceRegistrars = newSet(Key.get(new TypeLiteral<>() {
            }));
        }
        return resourceRegistrars;
    }

    @Deprecated(since = "4.0", forRemoval = true)
    SetBuilder<Object> contributeLegacyResources() {
        if (legacyResources == null) {
            legacyResources = newSet(Key.get(Object.class, JerseyModule.LEGACY_RESOURCES_BINDING));
        }
        return legacyResources;
    }

    @Deprecated(since = "4.0", forRemoval = true)
    MapBuilder<String, Object> contributeLegacyResourcesByPath() {
        if (legacyResourcesByPath == null) {
            legacyResourcesByPath = newMap(String.class, Object.class, JerseyModule.RESOURCES_PATH_OVERRIDE_BINDING);
        }

        return legacyResourcesByPath;
    }

    SetBuilder<Package> contributePackages() {
        if (packages == null) {
            packages = newSet(Package.class, JerseyModule.RESOURCE_PACKAGES_BINDING);
        }
        return packages;
    }

    MapBuilder<Class<?>, ParamConverter<?>> contributeParamConverters() {
        if (paramConverters == null) {
            paramConverters = newMap(
                    new TypeLiteral<>() {
                    },
                    new TypeLiteral<>() {
                    });
        }
        return paramConverters;
    }
}
