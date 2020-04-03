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

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

class OAuth2TokenAuthenticator implements ClientRequestFilter {

    private OAuth2TokenDAO tokenDAO;
    private volatile OAuth2Token lastToken;

    public OAuth2TokenAuthenticator(OAuth2Token initialToken, OAuth2TokenDAO tokenDAO) {
        this.tokenDAO = tokenDAO;
        this.lastToken = initialToken;
    }

    static String createAuthHeader(String token) {
        return "Bearer " + token;
    }

    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        headers.add("Authorization", getAuthorization());
    }

    protected String getAuthorization() {

        if (lastToken.needsRefresh()) {
            synchronized (this) {
                if (lastToken.needsRefresh()) {
                    lastToken = tokenDAO.getToken();
                }
            }
        }

        return createAuthHeader(lastToken.getAccessToken());
    }
}
