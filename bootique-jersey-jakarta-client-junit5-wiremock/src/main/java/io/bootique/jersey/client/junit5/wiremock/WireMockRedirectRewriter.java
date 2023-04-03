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

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.extension.StubMappingTransformer;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.http.HttpHeader.httpHeader;
import static com.github.tomakehurst.wiremock.http.HttpStatus.isRedirection;

class WireMockRedirectRewriter {
    private static final String LOCATION_HEADER_KEY = "Location";
    private static final String BOUTIQUE_ORIGINAL_LOCATION_HEADER_KEY = "BQ-Original-Location";

    private final String targetUrl;

    public WireMockRedirectRewriter(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    /**
     * 1. Saves value from the "Location" header to the temporary header "BQ-Original-Location".
     * 2. Builds a new redirect location that points to wiremock proxy, and saves it to "Location" header.
     * Is executed on every response.
     */
    public ResponseTransformer injector() {
        return new ResponseTransformer() {

            @Override
            public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
                if (!isRedirection(response.getStatus())) {
                    return response;
                }

                HttpHeaders headers = response.getHeaders();
                HttpHeader locationHeader = headers.getHeader(LOCATION_HEADER_KEY);

                // checking that redirect belongs to target resource
                return locationHeader.isPresent() && locationHeader.firstValue().startsWith(targetUrl)
                        ? rewriteRedirectResponse(request, response)
                        : response;
            }

            @Override
            public String getName() {
                return "bq-original-location-injector";
            }

            private Response rewriteRedirectResponse(Request request, Response response) {

                HttpHeaders headers = response.getHeaders();
                HttpHeader locationHeader = headers.getHeader(LOCATION_HEADER_KEY);

                URI requestUri = URI.create(request.getAbsoluteUrl());

                URI proxiedRedirectUrl = replaceSchemeAndAuthority(
                        URI.create(locationHeader.firstValue()), requestUri.getScheme(), requestUri.getRawAuthority());

                HttpHeaders newHeaders = overrideLocationHeaders(headers, proxiedRedirectUrl.toString())
                        .plus(httpHeader(BOUTIQUE_ORIGINAL_LOCATION_HEADER_KEY, locationHeader.firstValue()));

                return Response.Builder.like(response)
                        .but()
                        .headers(newHeaders)
                        .build();
            }

            private URI replaceSchemeAndAuthority(URI source, String newScheme, String newAuthority) {
                try {
                    return new URI(newScheme, newAuthority, source.getRawPath(), source.getRawQuery(), source.getRawFragment());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }


    /**
     * Reverts the "Location" redirect URL back to original and removes temporary "BQ-Original-Location" header.
     * Is executed after {@link #injector()} right before saving snapshot to file. It isn't executed when reading from cached files.
     */
    public StubMappingTransformer replacer() {

        return new StubMappingTransformer() {

            @Override
            public String getName() {
                return "bq-original-location-replacer";
            }

            @Override
            public StubMapping transform(StubMapping stubMapping, FileSource files, Parameters parameters) {
                var response = stubMapping.getResponse();

                if (!isRedirection(response.getStatus())) {
                    return stubMapping;
                }

                HttpHeaders headers = response.getHeaders();
                HttpHeader originalLocation = headers.getHeader(BOUTIQUE_ORIGINAL_LOCATION_HEADER_KEY);

                if (!originalLocation.isPresent()) {
                    return stubMapping;
                }

                var newHeaders = overrideLocationHeaders(headers, originalLocation.firstValue());

                var newRespDef = new ResponseDefinition(
                        response.getStatus(),
                        response.getStatusMessage(),
                        new Body(response.getBody()),
                        response.getBodyFileName(),
                        newHeaders,
                        response.getAdditionalProxyRequestHeaders(),
                        response.getFixedDelayMilliseconds(),
                        response.getDelayDistribution(),
                        response.getChunkedDribbleDelay(),
                        response.getProxyBaseUrl(),
                        response.getProxyUrlPrefixToRemove(),
                        response.getFault(),
                        response.getTransformers(),
                        response.getTransformerParameters(),
                        response.wasConfigured());

                return new StubMapping(stubMapping.getRequest(), newRespDef);
            }
        };
    }


    private static HttpHeaders overrideLocationHeaders(HttpHeaders headers, String newLocation) {
        List<HttpHeader> filtered = headers
                .all()
                .stream()
                .filter(it -> !(LOCATION_HEADER_KEY.equals(it.key()) || BOUTIQUE_ORIGINAL_LOCATION_HEADER_KEY.equals(it.key())))
                .collect(Collectors.toList());

        return new HttpHeaders(filtered)
                .plus(httpHeader(LOCATION_HEADER_KEY, newLocation));
    }
}
