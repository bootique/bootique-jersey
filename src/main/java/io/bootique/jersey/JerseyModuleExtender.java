package io.bootique.jersey;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;

public class JerseyModuleExtender {

    private Binder binder;

    private Multibinder<Feature> features;
    private Multibinder<DynamicFeature> dynamicFeatures;
    private Multibinder<Object> resources;
    private Multibinder<Package> packages;

    JerseyModuleExtender(Binder binder) {
        this.binder = binder;
    }

    JerseyModuleExtender initAllExtensions() {
        contributeDynamicFeatures();
        contributeFeatures();
        contributePackages();
        contributeResources();

        return this;
    }

    public JerseyModuleExtender addPackage(Package aPackage) {
        contributePackages().addBinding().toInstance(aPackage);
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

    public <T extends  DynamicFeature> JerseyModuleExtender addDynamicFeature(Class<T> featureType) {
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

    protected Multibinder<Feature> contributeFeatures() {
        if (features == null) {
            features = Multibinder.newSetBinder(binder, Feature.class);
        }
        return features;
    }

    protected Multibinder<DynamicFeature> contributeDynamicFeatures() {
        if (dynamicFeatures == null) {
            dynamicFeatures = Multibinder.newSetBinder(binder, DynamicFeature.class);
        }
        return dynamicFeatures;
    }

    protected Multibinder<Object> contributeResources() {
        if (resources == null) {
            resources = Multibinder.newSetBinder(binder, Key.get(Object.class, JerseyResource.class));
        }
        return resources;
    }

    protected Multibinder<Package> contributePackages() {
        if (packages == null) {
            packages = Multibinder.newSetBinder(binder, Package.class);
        }
        return packages;
    }
}
