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

import javax.ws.rs.client.WebTarget;

/**
 * An injectable manager of preconfigured JAX RS {@link WebTarget} objects.
 */
public interface HttpTargets {

    /**
     * Returns a new {@link WebTarget} object associated with a named configuration, that can be used to send
     * requests to a given HTTP endpoint. This method allows to delegate HTTP endpoint configuration to the Bootique
     * configuration subsystem instead of doing it in the code.
     *
     * @param targetName a symbolic name of the returned target associated with configuration under
     *                   "jerseyclient.targets".
     * @return a new {@link WebTarget} associated with a named configuration, that can be used to send requests to a
     * given HTTP endpoint.
     */
    WebTarget newTarget(String targetName);
}
