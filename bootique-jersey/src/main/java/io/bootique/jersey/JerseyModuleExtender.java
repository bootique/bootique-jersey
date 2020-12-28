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

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import java.util.Map;

public class JerseyModuleExtender extends ModuleExtender<JerseyModuleExtender> {

    private SetBuilder<Feature> features;
    private SetBuilder<DynamicFeature> dynamicFeatures;
    private SetBuilder<Object> resources;
    private SetBuilder<Package> packages;
    private SetBuilder<MappedResource> mappedResources;
    private MapBuilder<String, Object> resourcesByPath;
    private MapBuilder<String, Object> properties;

    JerseyModuleExtender(Binder binder) {
        super(binder);
    }

    public JerseyModuleExtender initAllExtensions() {
        contributeDynamicFeatures();
        contributeFeatures();
        contributePackages();
        contributeResources();
        contributeResourcesByPath();
        contributeMappedResources();
        contributeProperties();

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

    public JerseyModuleExtender addResource(Object resource) {
        contributeResources().addInstance(resource);
        return this;
    }

    public JerseyModuleExtender addResource(Class<?> resource) {
        contributeResources().add(resource);
        return this;
    }

    /**
     * Registers an API resource type with a custom path. The path argument overrides class-level
     * {@link javax.ws.rs.Path} annotation value, allowing to remap resource to a different URL. This even allows to
     * map the same resource multiple times under different paths.
     *
     * @param resource type of the resource
     * @param path     resource URL path that overrides {@link javax.ws.rs.Path} annotation on the resource class.
     * @return this extender
     * @since 2.0
     */
    public JerseyModuleExtender addResource(Class<?> resource, String path) {
        contributeResourcesByPath().put(path, resource);
        return this;
    }

    /**
     * Registers a "mapped" API resource. MappedResource paths override class-level {@link javax.ws.rs.Path} annotation
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
     * Registers a "mapped" API resource. MappedResource paths override class-level {@link javax.ws.rs.Path} annotation
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
     * Registers a "mapped" API resource. MappedResource paths override class-level {@link javax.ws.rs.Path} annotation
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
     * @since 2.0.B1
     */
    public JerseyModuleExtender setApplication(Application app) {
        binder.bind(Application.class).toInstance(app);
        return this;
    }

    /**
     * @since 2.0.B1
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
     * @since 0.22
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
     * @since 0.22
     */
    public JerseyModuleExtender setProperties(Map<String, String> properties) {
        properties.forEach(this::setProperty);
        return this;
    }

    protected MapBuilder<String, Object> contributeResourcesByPath() {
        if (resourcesByPath == null) {
            resourcesByPath = newMap(String.class, Object.class, JerseyModule.RESOURCES_BY_PATH_BINDING);
        }
        return resourcesByPath;
    }


    protected SetBuilder<MappedResource> contributeMappedResources() {
        if (mappedResources == null) {
            mappedResources = newSet(MappedResource.class);
        }
        return mappedResources;
    }

    protected MapBuilder<String, Object> contributeProperties() {
        if (properties == null) {
            // TODO: switch to named bindings defined in JerseyModule?
            properties = newMap(String.class, Object.class, JerseyResource.class);
        }
        return properties;
    }

    protected SetBuilder<Feature> contributeFeatures() {
        if (features == null) {
            features = newSet(Feature.class);
        }
        return features;
    }

    protected SetBuilder<DynamicFeature> contributeDynamicFeatures() {
        if (dynamicFeatures == null) {
            dynamicFeatures = newSet(DynamicFeature.class);
        }
        return dynamicFeatures;
    }

    protected SetBuilder<Object> contributeResources() {
        if (resources == null) {
            // TODO: switch to named bindings defined in JerseyModule?
            resources = newSet(Key.get(Object.class, JerseyResource.class));
        }
        return resources;
    }

    protected SetBuilder<Package> contributePackages() {
        if (packages == null) {
            // TODO: switch to named bindings defined in JerseyModule?
            packages = newSet(Package.class, JerseyResource.class);
        }
        return packages;
    }
}
