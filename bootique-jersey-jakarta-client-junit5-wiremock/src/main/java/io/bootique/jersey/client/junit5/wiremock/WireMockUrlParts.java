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
package io.bootique.jersey.client.junit5.wiremock;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * Provides different parts of the target URL of the WireMockTester used to build requests and store cached responses.
 *
 * @since 3.0
 */
public class WireMockUrlParts {

    private final String baseUrl;
    private final String path;
    private final String testerFolder;

    public static WireMockUrlParts of(String targetUrl) {
        Objects.requireNonNull(targetUrl);

        URL url;
        try {
            url = new URL(targetUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid 'targetUrl': " + targetUrl, e);
        }

        return new WireMockUrlParts(
                url.getProtocol() + "://" + url.getAuthority(),
                url.getPath(),
                convertToFolderName(url.getAuthority())
        );
    }

    protected WireMockUrlParts(String baseUrl, String path, String testerFolder) {
        this.baseUrl = baseUrl;
        this.path = path;
        this.testerFolder = testerFolder;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getPath() {
        return path;
    }

    public String getTesterFolder() {
        return testerFolder;
    }

    static String convertToFolderName(String url) {
        int len = url.length();
        StringBuilder out = new StringBuilder(len);

        boolean wasReplaced = false;
        for (int i = 0; i < len; i++) {
            char c = url.charAt(i);
            switch (c) {
                case '.':
                case ':':
                case '/':
                case '?':
                case '&':

                    // squash repeating _'s
                    if (!wasReplaced) {
                        out.append('_');
                        wasReplaced = true;
                    }

                    break;

                default:
                    wasReplaced = false;
                    out.append(c);
                    break;
            }
        }

        return out.toString();
    }
}
