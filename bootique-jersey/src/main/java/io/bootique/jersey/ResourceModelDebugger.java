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

import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Configuration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Debugs all container resources. In DEBUG mode prints only the app resources, in TRACE mode prints app and Jersey
 * internal resources as well as handler classes.
 *
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class ResourceModelDebugger implements ModelProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceModelDebugger.class);

    @Override
    public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {

        if (LOGGER.isDebugEnabled()) {
            List<ReportedResource> resources = new ArrayList<>();
            resourceModel.getRuntimeResourceModel().getRuntimeResources().forEach(r -> appendReported(resources, r));

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("The following HTTP resources are available:");
                resources.stream().sorted().forEach(this::traceResource);
            } else {
                LOGGER.debug("The following HTTP resources are available:");
                resources.stream().sorted().forEach(this::debugResource);
            }
        }

        return resourceModel;
    }

    private void traceResource(ReportedResource resource) {
        resource.methods
                .stream()
                .sorted()
                .forEach(rm -> LOGGER.trace("    {} {} {}", rm.method, resource.path, rm.handler.getName()));
    }

    private void debugResource(ReportedResource resource) {
        resource.methods
                .stream()
                // exclude Jersey internal WADL handlers in DEBUG mode (will include in TRACE mode)
                .filter(rm -> !rm.handler.getName().startsWith("org.glassfish.jersey.server.wadl"))
                .sorted()
                .forEach(rm -> LOGGER.debug("    {} {}", rm.method, resource.path));
    }

    private void appendReported(List<ReportedResource> appendTo, RuntimeResource rt) {

        // Only show resources that can be accessed by clients, but still process children of the empty resources
        if (!rt.getResourceMethods().isEmpty()) {
            toReported(rt).forEach(appendTo::add);
        }

        rt.getChildRuntimeResources().forEach(r -> appendReported(appendTo, r));
    }

    private List<ReportedResource> toReported(RuntimeResource rt) {
        return rt.getResources().stream().map(this::toReported).collect(Collectors.toList());
    }

    private ReportedResource toReported(Resource r) {

        String path = appendPath(new StringBuilder(), r).toString();
        List<ReportedResourceMethod> methods = r.getResourceMethods()
                .stream()
                .map(rm -> new ReportedResourceMethod(rm.getHttpMethod(), rm.getInvocable().getHandler().getHandlerClass()))
                .collect(Collectors.toList());

        return new ReportedResource(path, methods);
    }

    private StringBuilder appendPath(StringBuilder path, Resource r) {

        if (r.getParent() != null) {
            appendPath(path, r.getParent());
        }

        String p = r.getPath();
        if (!p.startsWith("/")) {
            path.append("/");
        }

        path.append(p);

        return path;
    }

    @Override
    public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
        return subResourceModel;
    }

    static class ReportedResource implements Comparable<ReportedResource> {
        final String path;
        final List<ReportedResourceMethod> methods;

        ReportedResource(String path, List<ReportedResourceMethod> methods) {
            this.path = path;
            this.methods = methods;
        }

        @Override
        public int compareTo(ResourceModelDebugger.ReportedResource o) {
            return path.compareTo(o.path);
        }
    }

    static class ReportedResourceMethod implements Comparable<ReportedResourceMethod> {
        final String method;
        final Class<?> handler;

        ReportedResourceMethod(String method, Class<?> handler) {
            this.method = method;
            this.handler = handler;
        }

        @Override
        public int compareTo(ResourceModelDebugger.ReportedResourceMethod o) {
            return method.compareTo(o.method);
        }
    }
}

