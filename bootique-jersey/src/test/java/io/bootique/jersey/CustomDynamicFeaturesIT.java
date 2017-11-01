package io.bootique.jersey;

import io.bootique.jetty.test.junit.JettyTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import static org.junit.Assert.assertTrue;

public class CustomDynamicFeaturesIT {

    @Rule
    public JettyTestFactory testFactory = new JettyTestFactory();

    @Test
    public void testFeaturesLoaded() {

        testFactory.app().autoLoadModules()
                .module(b -> JerseyModule
                        .extend(b)
                        .addDynamicFeature(DynamicFeature1.class)
                        .addDynamicFeature(DynamicFeature2.class))
                .start();

        assertTrue(DynamicFeature1.LOADED);
        assertTrue(DynamicFeature2.LOADED);
    }

    static class DynamicFeature1 implements DynamicFeature {

        static boolean LOADED = false;

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            LOADED = true;
        }
    }

    static class DynamicFeature2 implements DynamicFeature {

        static boolean LOADED = false;

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            LOADED = true;
        }
    }

}
