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

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

/**
 * @since 3.0
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
@Priority(100)
public class RequestLogger implements ClientRequestFilter, ClientResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\"{} {}\"", requestContext.getMethod(), requestContext.getUri());
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("\"{} {}\" {}", requestContext.getMethod(), requestContext.getUri(), responseContext.getStatus());
        }
    }
}