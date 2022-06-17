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
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.Collections;
import java.util.Set;

/**
 * A YAML-configurable factory of Jersey servlet.
 */
@BQConfig("Configures a servlet that is an entry point to Jersey REST API engine.")
public class JerseyServletFactory {

    private static final String DEFAULT_URL_PATTERN = "/*";

    protected String urlPattern;

    /**
     * @param urlPattern a URL: pattern for the Jersey servlet. Default is "/*".
     */
    @BQConfigProperty("Optional URL pattern for all Jersey resources within the webapp. Follows syntax and rules for " +
            "servlet 'url-pattern' described in the Servlet spec. Default pattern is '/*'. It will take over the " +
            "entire URL space of the webapp for Jersey. A non-default pattern allows to separate Jersey URL space from " +
            "other servlets. E.g. '/api/*' will result in URLs of all resources prefixed with '/api'. ")
    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    /**
     * Conditionally initializes servlet url pattern if it is null.
     *
     * @param urlPattern a URL: pattern for the Jersey servlet unless it was already
     *                   set.
     * @return self.
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
        Set<String> urlPatterns = Collections.singleton(getUrlPattern(resourceConfig));
        return new MappedServlet<>(servlet, urlPatterns, "jersey");
    }

    protected String getUrlPattern(ResourceConfig resourceConfig) {

        // explicit definition overrides annotation-defined path
        if (this.urlPattern != null) {
            return urlPattern;
        }

        String fromAnnotation = getUrlPatternFromAnnotation(resourceConfig);
        return fromAnnotation != null ? fromAnnotation : DEFAULT_URL_PATTERN;
    }

    protected String getUrlPatternFromAnnotation(ResourceConfig resourceConfig) {
        Application app = resourceConfig.getApplication();
        if (app != resourceConfig) {

            ApplicationPath a = app.getClass().getAnnotation(ApplicationPath.class);
            if (a != null) {
                String path = a.value();
                if (path != null) {
                    return normalizeAppPath(path);
                }
            }
        }

        return null;
    }

    protected String normalizeAppPath(String path) {
        // TODO: %-encode per ApplicationPath javadoc?

        StringBuilder normal = new StringBuilder();
        if (!path.startsWith("/")) {
            normal.append('/');
        }

        normal.append(path);

        if (path.length() > 0 && !path.endsWith("/")) {
            normal.append('/');
        }

        normal.append('*');

        return normal.toString();
    }
}
