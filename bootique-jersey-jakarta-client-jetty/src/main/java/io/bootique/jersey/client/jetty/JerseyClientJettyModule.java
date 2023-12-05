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
package io.bootique.jersey.client.jetty;

import io.bootique.BQCoreModule;
import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.di.Binder;
import io.bootique.jersey.client.JerseyClientModule;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JerseyClientJettyModule implements BQModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(JerseyClientJettyModule.class);

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Integrates Jetty transport for Jersey HTTP client")
                .build();
    }

    @Override
    public void configure(Binder binder) {
        JerseyClientModule.extend(binder).setConnectorProvider(JettyConnectorProvider.class);

        // Handling a bug / limitation in Jersey Jetty client. The line below in HttpClientFactoryFactory will cause
        // the Jetty transport to blow up. So force-change Bootique compression flag default.
        //
        //   config.register(new EncodingFeature(GZipEncoder.class));

        LOGGER.info("Forcing 'bq.jerseyclient.compression' to 'false', as Jetty HttpClient does its own compression handling. " +
                "Make sure your own configuration doesn't reset it back to true, as it will result in 'Not in GZIP format' exception");
        BQCoreModule.extend(binder).setProperty("bq.jerseyclient.compression", "false");
    }
}
