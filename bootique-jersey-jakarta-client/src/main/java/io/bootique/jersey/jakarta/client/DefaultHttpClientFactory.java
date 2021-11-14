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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.glassfish.jersey.client.ClientConfig;

import java.security.KeyStore;
import java.util.Map;

public class DefaultHttpClientFactory implements HttpClientFactory {

    private ClientConfig config;
    private Map<String, ClientRequestFilter> authFilters;
    private Map<String, KeyStore> trustStores;

    public DefaultHttpClientFactory(
            ClientConfig config,
            Map<String, ClientRequestFilter> authFilters,
            Map<String, KeyStore> trustStores) {

        this.authFilters = authFilters;
        this.config = config;
        this.trustStores = trustStores;
    }

    @Override
    public HttpClientBuilder newBuilder() {
        ClientBuilder builderDelegate = ClientBuilder.newBuilder().withConfig(config);

        return new DefaultHttpClientBuilder(builderDelegate);
    }

    private ClientRequestFilter namedAuth(String name) {
        ClientRequestFilter filter = authFilters.get(name);
        if (filter == null) {
            throw new IllegalArgumentException("No authenticator configured for name: " + name);
        }

        return filter;
    }

    private KeyStore namedTrustStore(String name) {
        KeyStore trustStore = trustStores.get(name);
        if (trustStore == null) {
            throw new IllegalArgumentException("No truststore configured for name: " + name);
        }

        return trustStore;
    }

    public class DefaultHttpClientBuilder implements HttpClientBuilder {

        private ClientBuilder delegate;

        public DefaultHttpClientBuilder(ClientBuilder delegate) {
            this.delegate = delegate;
        }

        public Client build() {
            return delegate.build();
        }

        public HttpClientBuilder auth(String authName) {
            delegate.register(namedAuth(authName));
            return this;
        }

        public HttpClientBuilder trustStore(String trustStoreName) {
            delegate.trustStore(namedTrustStore(trustStoreName));
            return this;
        }
    }
}
