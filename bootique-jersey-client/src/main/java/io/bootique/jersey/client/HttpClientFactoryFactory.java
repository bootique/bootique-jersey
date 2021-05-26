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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.di.BQInject;
import io.bootique.di.Injector;
import io.bootique.jersey.client.auth.AuthenticatorFactory;
import io.bootique.jersey.client.log.RequestLoggingFilter;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.JustInTimeInjectionResolver;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.EncodingFeature;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.message.GZipEncoder;

import javax.inject.Singleton;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.GenericType;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@BQConfig("Configures HttpClientFactory, including named authenticators, timeouts, SSL certificates, etc.")
public class HttpClientFactoryFactory {

    boolean followRedirects;
    boolean compression;
    int readTimeoutMs;
    int connectTimeoutMs;
    int asyncThreadPoolSize;
    Map<String, AuthenticatorFactory> auth;
    Map<String, TrustStoreFactory> trustStores;
    Map<String, WebTargetFactory> targets;

    public HttpClientFactoryFactory() {
        this.followRedirects = true;
        this.compression = true;
    }

    /**
     * @param auth a map of AuthenticationFactory instances by symbolic name.
     */
    @BQConfigProperty("A map of named \"authenticators\" for HTTP services that require authentication.")
    public void setAuth(Map<String, AuthenticatorFactory> auth) {
        this.auth = auth;
    }

    @BQConfigProperty("Sets whether the client should autromatically follow redirects. The default is 'true'.")
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    @BQConfigProperty("Sets the read timeout. The default (0) means no timeout.")
    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    @BQConfigProperty("Sets the connect timeout. The default (0) means no timeout.")
    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    @BQConfigProperty("Sets the size of the async requests thread pool. The default (0) sets no limit on the pool.")
    public void setAsyncThreadPoolSize(int asyncThreadPoolSize) {
        this.asyncThreadPoolSize = asyncThreadPoolSize;
    }

    /**
     * Sets a map of named client trust store factories.
     *
     * @param trustStores a map of named trust store factories.
     */
    @BQConfigProperty
    public void setTrustStores(Map<String, TrustStoreFactory> trustStores) {
        this.trustStores = trustStores;
    }

    /**
     * Enables or disables client-side compression headers. True by default.
     *
     * @param compression whether compression should be requested.
     */
    @BQConfigProperty
    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    /**
     * Sets a map of named target factories. This allows to define remote endpoints completely via configuration.
     *
     * @param targets a map of named target factories.
     */
    @BQConfigProperty
    public void setTargets(Map<String, WebTargetFactory> targets) {
        this.targets = targets;
    }

    /**
     * Creates and returns a new HttpTargetFactory for the  set of targets preconfigured in this factory.
     *
     * @param clientFactory
     * @return a new HttpTargetFactory for the preconfigured set of targets.
     */
    public HttpTargets createTargets(HttpClientFactory clientFactory) {
        return new DefaultHttpTargets(createNamedTargets(clientFactory));
    }

    public HttpClientFactory createClientFactory(Injector injector, Set<Feature> features) {
        ClientConfig config = createConfig(features);

        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(injector).to(Injector.class)
                        .in(Singleton.class);
                bind(ClientBqInjectorBridge.class)
                        .to(JustInTimeInjectionResolver.class)
                        .in(Singleton.class);
                bind(ClientBqInjectInjector.class)
                        .to(new GenericType<InjectionResolver<BQInject>>(){})
                        .in(Singleton.class);
            }
        });

        return new DefaultHttpClientFactory(
                config,
                createAuthFilters(injector),
                createTrustStores());
    }

    protected Map<String, KeyStore> createTrustStores() {

        if (trustStores == null) {
            return Collections.emptyMap();
        }

        Map<String, KeyStore> keyStores = new HashMap<>();
        trustStores.forEach((k, v) -> keyStores.put(k, v.createTrustStore()));
        return keyStores;
    }

    protected Map<String, ClientRequestFilter> createAuthFilters(Injector injector) {
        Map<String, ClientRequestFilter> filters = new HashMap<>();

        if (auth != null) {
            auth.forEach((k, v) -> filters.put(k, v.createAuthFilter(injector)));
        }

        return filters;
    }

    protected ClientConfig createConfig(Set<Feature> features) {
        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.FOLLOW_REDIRECTS, followRedirects);
        config.property(ClientProperties.READ_TIMEOUT, readTimeoutMs);
        config.property(ClientProperties.CONNECT_TIMEOUT, connectTimeoutMs);
        config.property(ClientProperties.ASYNC_THREADPOOL_SIZE, asyncThreadPoolSize);

        features.forEach(config::register);

        if (compression) {
            config.register(new EncodingFeature(GZipEncoder.class));
        }

        configRequestLogging(config);

        return config;
    }

    protected void configRequestLogging(ClientConfig config) {
        config.register(new RequestLoggingFilter());
    }

    protected Map<String, Supplier<WebTarget>> createNamedTargets(HttpClientFactory clientFactory) {
        if (targets == null || targets.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Supplier<WebTarget>> suppliers = new HashMap<>();
        targets.forEach((n, f) -> suppliers.put(n, f.createWebTargetSupplier(clientFactory, compression)));

        return suppliers;
    }
}
