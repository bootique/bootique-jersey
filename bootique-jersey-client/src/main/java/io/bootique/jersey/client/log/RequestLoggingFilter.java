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

package io.bootique.jersey.client.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

public class RequestLoggingFilter implements  ClientRequestFilter, ClientResponseFilter {

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