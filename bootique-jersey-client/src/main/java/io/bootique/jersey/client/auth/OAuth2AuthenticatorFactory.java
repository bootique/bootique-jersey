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

package io.bootique.jersey.client.auth;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.di.Injector;
import io.bootique.jersey.client.HttpClientBuilder;
import io.bootique.jersey.client.HttpClientFactory;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
@JsonTypeName("oauth2")
@BQConfig("Authenticator for Oauth2 protocol. Includes URL of the OAuth token endpoint and " +
        "username/password that are exchanged for the token.")
public class OAuth2AuthenticatorFactory implements AuthenticatorFactory {

    protected String tokenUrl;
    protected String tokenTrustStore;
    protected String username;
    protected String password;
    protected Duration expiresIn;

    public OAuth2AuthenticatorFactory() {
        this.expiresIn = Duration.ofHours(1);
    }

    public String getUsername() {
        return username;
    }

    @BQConfigProperty("Login username. A part of the application credentials to obtain oauth token.")
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    @BQConfigProperty("Password. A part of the application credentials to obtain oauth token.")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    @BQConfigProperty("A URL of the OAuth2 Token API endpoint.")
    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    /**
     * @param tokenTrustStore the name of the trust store, as mapped in Jersey Client configuration.
     */
    @BQConfigProperty("An optional name of a mapped trust store to use with when requesting a token.")
    public void setTokenTrustStore(String tokenTrustStore) {
        this.tokenTrustStore = tokenTrustStore;
    }

    @BQConfigProperty("A duration value for default token expiration. Will only be used for oauth servers that do " +
            "not send 'expires_in' attribute explicitly. If not set, this value is 1 hr.")
    public void setExpiresIn(Duration expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public ClientRequestFilter createAuthFilter(Injector injector) {
        OAuth2TokenDAO tokenDAO = createOAuth2TokenDAO(injector);
        return new OAuth2TokenAuthenticator(OAuth2Token.expiredToken(), tokenDAO);
    }

    protected OAuth2TokenDAO createOAuth2TokenDAO(Injector injector) {
        Objects.requireNonNull(username, "OAuth2 'username' is not specified");
        Objects.requireNonNull(password, "OAuth2 'password' is not specified");
        Objects.requireNonNull(tokenUrl, "OAuth2 'tokenUrl' is not specified");

        // defer initialization until HttpClientFactory becomes available.
        Function<String, WebTarget> tokenTargetFactory = tokenUrl -> tokenTarget(tokenUrl, injector);
        return new OAuth2TokenDAO(tokenTargetFactory, tokenUrl, username, password, expiresIn);
    }

    protected WebTarget tokenTarget(String tokenUrl, Injector injector) {
        HttpClientBuilder builder = injector.getInstance(HttpClientFactory.class).newBuilder();

        if (tokenTrustStore != null) {
            builder = builder.trustStore(tokenTrustStore);
        }

        return builder.build().register(JacksonFeature.class).target(tokenUrl);
    }
}
