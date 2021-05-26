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

package io.bootique.jersey.client;

import javax.ws.rs.client.Client;

/**
 * An injectable factory for creating JAX RS clients based on Bootique configuration.
 */
public interface HttpClientFactory {

    /**
     * Returns a new instance of JAX-RS HTTP {@link Client} initialized using "jerseyclient" configuration subtree.
     *
     * @return a new instance of JAX-RS HTTP client initialized using
     * "jerseyclient" configuration subtree.
     */
    default Client newClient() {
        return newBuilder().build();
    }

    /**
     * A builder for a new client. Allows to create a client with Bootique configuration-driven settings and select
     * a preconfigured authentication and trust store.
     *
     * @return a builder for a new client.
     */
    HttpClientBuilder newBuilder();
}
