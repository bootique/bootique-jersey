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

package io.bootique.jersey.v3.client.log;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestLoggingFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final String REQUEST_PREFIX = "> ";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(getRequestMessage(requestContext).toString());
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(getResponseMessage(requestContext, responseContext).toString());
        }
    }

    protected StringBuilder getRequestMessage(ClientRequestContext requestContext) {
        return new StringBuilder()
                .append("Sending client request.")
                .append("\n").append(REQUEST_PREFIX).append(requestContext.getMethod()).append(" ")
                .append(requestContext.getUri().toASCIIString());
    }

    protected StringBuilder getResponseMessage(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        return new StringBuilder()
                .append(" Client response received.")
                .append(" \"").append(requestContext.getMethod()).append(" ")
                .append(requestContext.getUri().getAuthority()).append(requestContext.getUri().getPath())
                .append("\" ").append(" Status: ").append(responseContext.getStatus());
    }
}