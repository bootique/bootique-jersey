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

import jakarta.ws.rs.client.WebTarget;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class DefaultHttpTargets implements HttpTargets {

    private final Map<String, Supplier<WebTarget>> namedTargets;

    public DefaultHttpTargets(Map<String, Supplier<WebTarget>> namedTargets) {
        this.namedTargets = namedTargets;
    }

    @Override
    public WebTarget newTarget(String targetName) {
        return targetFactory(targetName).get();
    }

    protected Supplier<WebTarget> targetFactory(String name) {
        Supplier<WebTarget> supplier = namedTargets.get(name);

        if (supplier == null) {
            throw new IllegalArgumentException("No target configured for name: " + name);
        }

        return supplier;
    }

    @Override
    public Set<String> getTargetNames() {
        return namedTargets.keySet();
    }
}
