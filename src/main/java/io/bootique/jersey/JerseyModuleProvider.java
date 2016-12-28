package io.bootique.jersey;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.jetty.server.ServerFactory;

import java.util.Collections;
import java.util.Map;

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
    public Map<String, Class<?>> configs() {
        // TODO: config prefix is hardcoded. Refactor away from ConfigModule, and make provider
        // generate config prefix, reusing it in metadata...
        return Collections.singletonMap("jersey", JerseyServletFactory.class);
    }
}
