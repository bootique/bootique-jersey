/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.jersey;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import io.bootique.ModuleExtender;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import java.util.Map;

public class JerseyModuleExtender extends ModuleExtender<JerseyModuleExtender> {

    private Multibinder<Feature> features;
    private Multibinder<DynamicFeature> dynamicFeatures;
    private Multibinder<Object> resources;
    private Multibinder<Package> packages;
    private MapBinder<String, Object> properties;

    JerseyModuleExtender(Binder binder) {
        super(binder);
    }

    public JerseyModuleExtender initAllExtensions() {
        contributeDynamicFeatures();
        contributeFeatures();
        contributePackages();
        contributeResources();
        contributeProperties();

        return this;
    }

    public JerseyModuleExtender addPackage(Package aPackage) {
        contributePackages().addBinding().toInstance(aPackage);
        return this;
    }

    public JerseyModuleExtender addPackage(Class<?> anyClassInPackage) {
        contributePackages().addBinding().toInstance(anyClassInPackage.getPackage());
        return this;
    }

    public JerseyModuleExtender addFeature(Feature feature) {
        contributeFeatures().addBinding().toInstance(feature);
        return this;
    }

    public <T extends Feature> JerseyModuleExtender addFeature(Class<T> featureType) {
        contributeFeatures().addBinding().to(featureType);
        return this;
    }

    public JerseyModuleExtender addDynamicFeature(DynamicFeature feature) {
        contributeDynamicFeatures().addBinding().toInstance(feature);
        return this;
    }

    public <T extends DynamicFeature> JerseyModuleExtender addDynamicFeature(Class<T> featureType) {
        contributeDynamicFeatures().addBinding().to(featureType);
        return this;
    }


    public JerseyModuleExtender addResource(Object resource) {
        contributeResources().addBinding().toInstance(resource);
        return this;
    }

    public JerseyModuleExtender addResource(Class<?> resource) {
        contributeResources().addBinding().to(resource);
        return this;
    }

    /**
     * Sets Jersey container property. This allows setting ResourceConfig properties that can not be set via JAX RS features.
     *
     * @param name property name
     * @param value property value
     * @return
     * @see org.glassfish.jersey.server.ServerProperties
     * @since 0.22
     */
    public JerseyModuleExtender setProperty(String name, Object value) {
        contributeProperties().addBinding(name).toInstance(value);
        return this;
    }

    /**
     * Sets Jersey container properties.  This allows setting ResourceConfig properties that can not be set via JAX RS features.
     *
     * @param properties
     * @return this extender instance
     * @see org.glassfish.jersey.server.ServerProperties
     * @since 0.22
     */
    public JerseyModuleExtender setProperties(Map<String, String> properties) {
        properties.forEach(this::setProperty);
        return this;
    }

    protected MapBinder<String, Object> contributeProperties() {
        if (properties == null) {
            // should we use a more properly named annotation
            properties = newMap(String.class, Object.class, JerseyResource.class);
        }
        return properties;
    }

    protected Multibinder<Feature> contributeFeatures() {
        if (features == null) {
            features = newSet(Feature.class);
        }
        return features;
    }

    protected Multibinder<DynamicFeature> contributeDynamicFeatures() {
        if (dynamicFeatures == null) {
            dynamicFeatures = newSet(DynamicFeature.class);
        }
        return dynamicFeatures;
    }

    protected Multibinder<Object> contributeResources() {
        if (resources == null) {
            resources = newSet(Key.get(Object.class, JerseyResource.class));
        }
        return resources;
    }

    protected Multibinder<Package> contributePackages() {
        if (packages == null) {
            packages = newSet(Package.class);
        }
        return packages;
    }
}
