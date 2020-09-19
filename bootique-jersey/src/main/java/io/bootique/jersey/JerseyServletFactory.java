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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.jetty.MappedServlet;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.Collections;
import java.util.Set;

/**
 * A YAML-configurable factory of Jersey servlet.
 *
 * @since 0.10
 */
@BQConfig("Configures the servlet that is an entry point to Jersey REST API engine.")
public class JerseyServletFactory {

    private static final String URL_PATTERN = "/*";

    protected String urlPattern;

    /**
     * @param urlPattern a URL: pattern for the Jersey servlet. Default is "/*".
     * @since 0.11
     */
    @BQConfigProperty
    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    /**
     * Conditionally initializes servlet url pattern if it is null.
     *
     * @param urlPattern a URL: pattern for the Jersey servlet unless it was already
     *                   set.
     * @return self.
     * @since 0.11
     * @deprecated since 2.0 as we don't need to initialize the urlPattern explicitly to be able to returna  default.
     */
    @Deprecated
    public JerseyServletFactory initUrlPatternIfNotSet(String urlPattern) {

        if (this.urlPattern == null) {
            this.urlPattern = urlPattern;
        }

        return this;
    }

    public MappedServlet<ServletContainer> createJerseyServlet(ResourceConfig resourceConfig) {
        ServletContainer servlet = new ServletContainer(resourceConfig);
        Set<String> urlPatterns = Collections.singleton(getUrlPattern());
        return new MappedServlet<>(servlet, urlPatterns, "jersey");
    }

    protected String getUrlPattern() {
        return urlPattern != null ? urlPattern : URL_PATTERN;
    }
}
