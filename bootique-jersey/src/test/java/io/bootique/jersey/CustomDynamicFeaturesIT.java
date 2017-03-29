package io.bootique.jersey;

import com.google.inject.Module;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jersey.unit.BQJerseyTest;
import org.junit.Test;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class CustomDynamicFeaturesIT extends BQJerseyTest {

    @Override
    protected Module createTestModule() {
        return (b) -> {
            JerseyModule.extend(b)
                    .addDynamicFeature(DynamicFeature1.class)
                    .addDynamicFeature(DynamicFeature2.class);
            b.bind(ConfigurationFactory.class).toInstance(mock(ConfigurationFactory.class));
        };
    }

    @Test
    public void testFeaturesLoaded() {
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
