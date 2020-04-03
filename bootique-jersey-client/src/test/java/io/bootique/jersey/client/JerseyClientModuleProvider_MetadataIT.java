/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jersey.client;

import io.bootique.BQRuntime;
import io.bootique.meta.config.ConfigListMetadata;
import io.bootique.meta.config.ConfigMapMetadata;
import io.bootique.meta.config.ConfigMetadataNode;
import io.bootique.meta.config.ConfigMetadataVisitor;
import io.bootique.meta.config.ConfigObjectMetadata;
import io.bootique.meta.config.ConfigValueMetadata;
import io.bootique.meta.module.ModuleMetadata;
import io.bootique.meta.module.ModulesMetadata;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.Comparator;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JerseyClientModuleProvider_MetadataIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testMetadata() {

        BQRuntime runtime = testFactory.app().autoLoadModules().createRuntime();

        ModulesMetadata modulesMetadata = runtime.getInstance(ModulesMetadata.class);
        Optional<ModuleMetadata> jerseyClientOpt = modulesMetadata.getModules()
                .stream()
                .filter(m -> "JerseyClientModule".equals(m.getName()))
                .findFirst();

        assertTrue(jerseyClientOpt.isPresent());
        ModuleMetadata jerseyClient = jerseyClientOpt.get();

        assertTrue(jerseyClient.getDescription().startsWith("Provides configurable JAX-RS HTTP client with pluggable authentication."));

        assertEquals(1, jerseyClient.getConfigs().size());
        ConfigMetadataNode rootConfig = jerseyClient.getConfigs().stream().findFirst().get();

        assertEquals("jerseyclient", rootConfig.getName());

        String result = rootConfig.accept(new ConfigMetadataVisitor<String>() {

            @Override
            public String visitObjectMetadata(ConfigObjectMetadata metadata) {

                StringBuilder out = new StringBuilder(metadata.getName());

                metadata.getProperties()
                        .stream()
                        .sorted(Comparator.comparing(ConfigMetadataNode::getName))
                        .forEach(p -> out.append("[").append(p.accept(this)).append("]"));

                return out.toString();
            }

            @Override
            public String visitValueMetadata(ConfigValueMetadata metadata) {
                return metadata.getName() + ":" + metadata.getType().getTypeName();
            }

            @Override
            public String visitListMetadata(ConfigListMetadata metadata) {
                return "list:" + metadata.getName() + "<" + metadata.getElementType().getType().getTypeName() + ">";
            }

            @Override
            public String visitMapMetadata(ConfigMapMetadata metadata) {
                return "map:" + metadata.getName() + "<" + metadata.getKeysType().getTypeName() + "," +
                        metadata.getValuesType().getType().getTypeName() + ">";
            }
        });

        assertEquals("jerseyclient" +
                "[asyncThreadPoolSize:int]" +
                "[map:auth<java.lang.String,io.bootique.jersey.client.auth.AuthenticatorFactory>]" +
                "[compression:boolean]" +
                "[connectTimeoutMs:int]" +
                "[followRedirects:boolean]" +
                "[readTimeoutMs:int]" +
                "[map:targets<java.lang.String,io.bootique.jersey.client.WebTargetFactory>]" +
                "[map:trustStores<java.lang.String,io.bootique.jersey.client.TrustStoreFactory>]", result);
    }
}
