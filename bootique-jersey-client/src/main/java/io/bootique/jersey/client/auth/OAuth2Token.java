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

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a token received from a remote oauth server.
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class OAuth2Token {

    private String accessToken;
    private LocalDateTime refreshAfter;

    protected OAuth2Token(String accessToken, LocalDateTime refreshAfter) {
        this.accessToken = accessToken;
        this.refreshAfter = refreshAfter;
    }

    /**
     * A factory method for an expired token that can be used as a placeholder initial token.
     *
     * @return a token that can be used as an initial placeholder for authenticator.
     */
    public static OAuth2Token expiredToken() {
        return new OAuth2Token("*placeholder_token*", LocalDateTime.of(1970, 1, 1, 0, 0, 0));
    }

    public static OAuth2Token token(String accessToken, LocalDateTime expiresOn) {
        Objects.requireNonNull(accessToken, "'accessToken' is null");
        Objects.requireNonNull(expiresOn, "'expiresOn' is null");

        // refresh the token if it is still fresh, but is about to expire... The hope is this improves reliability.
        // Though in fact we have no idea... E.g. an attempt to refresh a token before it is expired may result in
        // the same token returned from the server (?) so this may be a dubious optimization...
        // TODO: Need to test with common oauth servers (Google, FB, GitHub), and maybe make configurable as "refreshDrift"  or something.
        LocalDateTime refreshAfter = Objects.requireNonNull(expiresOn).minusSeconds(2);
        return new OAuth2Token(accessToken, refreshAfter);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public boolean needsRefresh() {
        return refreshAfter.isBefore(LocalDateTime.now());
    }

    public LocalDateTime getRefreshAfter() {
        return refreshAfter;
    }
}
