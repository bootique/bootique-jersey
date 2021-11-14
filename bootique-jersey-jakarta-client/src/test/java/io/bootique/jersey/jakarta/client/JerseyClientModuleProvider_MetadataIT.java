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

package io.bootique.jersey.jakarta.client;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.meta.config.*;
import io.bootique.meta.module.ModuleMetadata;
import io.bootique.meta.module.ModulesMetadata;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class JerseyClientModuleProvider_MetadataIT {

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique.app().autoLoadModules().createRuntime();

    @Test
    public void testMetadata() {

        ModulesMetadata modulesMetadata = app.getInstance(ModulesMetadata.class);
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
                "[map:auth<java.lang.String,io.bootique.jersey.jakarta.client.auth.AuthenticatorFactory>]" +
                "[compression:boolean]" +
                "[connectTimeoutMs:int]" +
                "[followRedirects:boolean]" +
                "[readTimeoutMs:int]" +
                "[map:targets<java.lang.String,io.bootique.jersey.jakarta.client.WebTargetFactory>]" +
                "[map:trustStores<java.lang.String,io.bootique.jersey.jakarta.client.TrustStoreFactory>]", result);
    }
}
