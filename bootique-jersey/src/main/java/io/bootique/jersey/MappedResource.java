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
package io.bootique.jersey;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * @since 2.0
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class MappedResource<T> {

    private T resource;
    private Set<String> urlPatterns;

    public MappedResource(T resource, Set<String> urlPatterns) {
        this.resource = resource;
        this.urlPatterns = urlPatterns;
    }

    public MappedResource(T resource, String... urlPatterns) {
        this(resource, new HashSet<>(asList(urlPatterns)));
    }

    public T getResource() {
        return resource;
    }

    public Set<String> getUrlPatterns() {
        return urlPatterns;
    }
}
