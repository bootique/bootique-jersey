package io.bootique.jersey;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.jetty.JettyModuleProvider;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public class JerseyModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new JerseyModule();
    }

    /**
     * @return a single-entry map with {@link JerseyServletFactory}.
     * @since 0.19
     */
    @Override
    public Map<String, Type> configs() {
        // TODO: config prefix is hardcoded. Refactor away from ConfigModule, and make provider
        // generate config prefix, reusing it in metadata...
        return singletonMap("jersey", JerseyServletFactory.class);
    }

    @Override
    public Collection<BQModuleProvider> dependencies() {
        return singletonList(
                new JettyModuleProvider()
        );
    }
}
