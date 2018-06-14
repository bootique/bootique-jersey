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
