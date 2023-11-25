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

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Objects;

/**
 * @since 1.1
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
@JsonTypeName("apiKeyHeader")
@BQConfig("Performs request authentication with a fixed API key passed as a request header")
public class ApiKeyHeaderAuthenticatorFactory implements AuthenticatorFactory {

    protected String name;
    protected String key;

    @Override
    public ClientRequestFilter createAuthFilter(Injector injector) {
        String name = this.name != null ? this.name : "X-Api-Key";
        return new HeaderAuthenticator(name, key);
    }

    @BQConfigProperty("Header name to use for auth. If missing, the default of \"X-Api-Key\" will be used.")
    public void setName(String name) {
        this.name = name;
    }

    @BQConfigProperty("API Key value. Required.")
    public void setKey(String key) {
        this.key = key;
    }

    static class HeaderAuthenticator implements ClientRequestFilter {

        private String headerName;
        private String apiKey;

        public HeaderAuthenticator(String headerName, String apiKey) {
            this.headerName = Objects.requireNonNull(headerName, "Null auth header name");
            this.apiKey = Objects.requireNonNull(apiKey, "Null API key");
        }

        public void filter(ClientRequestContext requestContext) {
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            headers.add(headerName, apiKey);
        }
    }
}
