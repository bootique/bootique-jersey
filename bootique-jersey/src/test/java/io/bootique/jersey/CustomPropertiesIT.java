package io.bootique.jersey;

import com.google.inject.Module;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jersey.unit.BQJerseyTest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class CustomPropertiesIT extends BQJerseyTest {

    @Test
    public void testProperties() {
        ResourceConfig config = injector.getInstance(ResourceConfig.class);
        assertEquals(67, config.getProperty("test.x"));
    }

    @Override
    protected Module createTestModule() {
        return b -> {
            JerseyModule.extend(b).setProperty("test.x", 67);
            b.bind(ConfigurationFactory.class).toInstance(mock(ConfigurationFactory.class));
        };
    }
}
