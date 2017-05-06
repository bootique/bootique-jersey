package io.bootique.jersey.unit;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jersey.JerseyModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import javax.ws.rs.core.Application;

import static org.mockito.Mockito.mock;

/**
 * An abstract superclass of Bootique Jersey integration tests.
 */
public abstract class BQJerseyTest extends JerseyTest {

    protected Injector injector;

    @Override
    protected Application configure() {
        this.injector = Guice.createInjector(new JerseyModule(), createTestModule());
        return injector.getInstance(ResourceConfig.class);
    }

    protected Module createTestModule() {
        return (b) -> {
            b.bind(ConfigurationFactory.class).toInstance(mock(ConfigurationFactory.class));
        };
    }
}
