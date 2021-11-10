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

package io.bootique.jersey.v3.client;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.EncodingFeature;
import org.glassfish.jersey.message.GZipEncoder;

import java.util.Objects;
import java.util.function.Supplier;

@BQConfig
public class WebTargetFactory {

    private String url;
    private String auth;
    private String trustStore;

    // the next block of vars is overriding the values from the parent client config.
    // so they must use objects instead of primitives to maintain a distinction between "null" and "not set".
    private Boolean followRedirects;
    private Boolean compression;
    private Integer readTimeoutMs;
    private Integer connectTimeoutMs;


    @BQConfigProperty
    public void setUrl(String url) {
        this.url = url;
    }

    @BQConfigProperty("An optional name of the authentication config referencing one of the entries in 'jerseyclient.auth'.")
    public void setAuth(String auth) {
        this.auth = auth;
    }

    @BQConfigProperty("If set, overrides 'jerseyclient.followRedirects' - the redirect policy of the parent client config.")
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    @BQConfigProperty("If set, overrides 'jerseyclient.compression' - the compression policy of the parent client config.")
    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    @BQConfigProperty("If set, overrides 'jerseyclient.readTimeoutMs' - the read timeout setting of the parent client config.")
    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    @BQConfigProperty("If set, overrides 'jerseyclient.connectTimeoutMs' - the connect timeout setting of the parent client config.")
    public void setConnectTimeoutMs(Integer connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    @BQConfigProperty("An optional name of the trust store config referencing one of the entries in 'jerseyclient.trusStores'.")
    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    // "compression" is not JAX-RS property, so it is hard to tell whether the parent enabled it or not.
    // The solution here is to accept parent compression as an explicit parameter
    public Supplier<WebTarget> createWebTargetSupplier(HttpClientFactory clientFactory, boolean parentCompression) {

        // copy vars for the supplier (with minimal validation), so that they can not be overridden by the time
        // supplier is executed...

        String localUrl = Objects.requireNonNull(url, "'url' property is required");
        Boolean followRedirectsOverride = this.followRedirects;
        Integer readTimeoutMsOverride = this.readTimeoutMs;
        Integer connectTimeoutMsOverride = this.connectTimeoutMs;

        // ensure we don't register compression feature twice
        boolean enableCompression = this.compression != null && this.compression && !parentCompression;

        // preconfigure client ... it will be the factory for targets
        HttpClientBuilder builder = clientFactory.newBuilder();

        if (auth != null) {
            builder.auth(auth);
        }

        if (trustStore != null) {
            builder.trustStore(trustStore);
        }

        Client client = builder.build();

        return () -> {
            WebTarget target = client.target(localUrl);

            if (followRedirectsOverride != null) {
                target = target.property(ClientProperties.FOLLOW_REDIRECTS, followRedirectsOverride);
            }

            if (readTimeoutMsOverride != null) {
                target = target.property(ClientProperties.READ_TIMEOUT, readTimeoutMsOverride);
            }

            if(connectTimeoutMsOverride != null) {
                target = target.property(ClientProperties.CONNECT_TIMEOUT, connectTimeoutMsOverride);
            }

            if (enableCompression) {
                target = target.register(new EncodingFeature(GZipEncoder.class));
            }

            return target;
        };
    }
}
