package io.bootique.jersey;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.bootique.BQRuntime;
import io.bootique.jetty.MappedServlet;
import io.bootique.test.junit.BQTestFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JerseyModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testDefaultContents() {
        BQRuntime runtime = testFactory.app().createRuntime();

        assertNotNull(runtime.getInstance(ResourceConfig.class));

        TypeLiteral<MappedServlet<ServletContainer>> jerseyServletKey = new TypeLiteral<MappedServlet<ServletContainer>>() {
        };

        assertNotNull(runtime.getInstance(Key.get(jerseyServletKey)));
    }

    @Test
    public void testProperties() {

        BQRuntime runtime = testFactory.app()
                .autoLoadModules()
                .module(b -> JerseyModule.extend(b).setProperty("test.x", 67))
                .createRuntime();

        ResourceConfig config = runtime.getInstance(ResourceConfig.class);
        assertEquals(67, config.getProperty("test.x"));
    }

    @Test
    public void testNoResourcesModule() {
        BQRuntime runtime = testFactory.app().module(JerseyModule.class).createRuntime();
        assertNotNull(runtime.getInstance(ResourceConfig.class));
    }
}
