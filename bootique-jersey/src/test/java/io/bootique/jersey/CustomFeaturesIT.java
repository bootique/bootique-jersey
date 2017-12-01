package io.bootique.jersey;

import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import static org.junit.Assert.assertTrue;

public class CustomFeaturesIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testFeaturesLoaded() {

        testFactory.app("-s")
                .module(b -> JerseyModule.extend(b).addFeature(Feature1.class).addFeature(Feature2.class))
                .run();

        assertTrue(Feature1.LOADED);
        assertTrue(Feature2.LOADED);
    }

    static class Feature1 implements Feature {

        static boolean LOADED = false;

        @Override
        public boolean configure(FeatureContext c) {
            LOADED = true;
            return true;
        }
    }

    static class Feature2 implements Feature {

        static boolean LOADED = false;

        @Override
        public boolean configure(FeatureContext c) {
            LOADED = true;
            return true;
        }
    }
}
