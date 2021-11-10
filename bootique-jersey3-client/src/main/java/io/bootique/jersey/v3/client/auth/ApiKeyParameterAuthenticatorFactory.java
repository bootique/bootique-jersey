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
package io.bootique.jersey.v3.client.auth;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.di.Injector;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;
import java.util.Objects;

/**
 * @since 1.1
 */
@JsonTypeName("apiKeyParameter")
@BQConfig("Performs request authentication with a fixed API key passed as a query parameter")
public class ApiKeyParameterAuthenticatorFactory implements AuthenticatorFactory {

    protected String name;
    protected String key;

    @Override
    public ClientRequestFilter createAuthFilter(Injector injector) {
        String name = this.name != null ? this.name : "api_key";
        return new ParamAuthenticator(name, key);
    }

    @BQConfigProperty("Parameter name to use for auth. If missing, the default of \"api_key\" will be used.")
    public void setName(String name) {
        this.name = name;
    }

    @BQConfigProperty("API Key value. Required.")
    public void setKey(String key) {
        this.key = key;
    }

    static class ParamAuthenticator implements ClientRequestFilter {

        private String paramName;
        private String authKey;

        public ParamAuthenticator(String paramName, String apiKey) {
            this.paramName = Objects.requireNonNull(paramName, "Null auth param name");
            this.authKey = Objects.requireNonNull(apiKey, "Null API key");
        }

        public void filter(ClientRequestContext requestContext) {

            URI uri = UriBuilder.fromUri(requestContext.getUri())
                    // hmmm.. should we complain if "paramName" is already in the query?
                    .replaceQueryParam(paramName, authKey)
                    .build();

            requestContext.setUri(uri);
        }

    }
}

