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
package io.bootique.jersey.jakarta;

import jakarta.ws.rs.core.Configuration;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;

import java.util.*;

/**
 * Defines custom resource mappings
 *
 * @since 2.0
 */
public class ResourcePathCustomizer implements ModelProcessor {

    private Map<Class<?>, List<String>> pathsByType;

    protected ResourcePathCustomizer(Map<Class<?>, List<String>> pathsByType) {
        this.pathsByType = pathsByType;
    }

    public static ResourcePathCustomizer create(Set<MappedResource> mappedResources, Map<String, Object> resourcesByPath) {
        if (mappedResources.isEmpty() && resourcesByPath.isEmpty()) {
            throw new IllegalArgumentException("No resources to override");
        }
        Map<Class<?>, List<String>> index = new HashMap<>();

        mappedResources.forEach(mr -> index.computeIfAbsent(mr.getResource().getClass(), c -> new ArrayList<>(2)).addAll(mr.getUrlPatterns()));
        resourcesByPath.forEach((p, o) -> index.computeIfAbsent(o.getClass(), c -> new ArrayList<>(2)).add(p));

        return new ResourcePathCustomizer(index);
    }

    @Override
    public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {

        // seems impossible to get a hold of a resource *instance* within the ResourceModel.. So trying to locate
        // resources that require a path override by type... Any drawbacks?

        ResourceModel.Builder modelBuilder = new ResourceModel.Builder(false);
        resourceModel.getResources().forEach(r -> process(modelBuilder, r));
        return modelBuilder.build();
    }

    protected void process(ResourceModel.Builder modelBuilder, Resource resource) {

        for (Class<?> type : resource.getHandlerClasses()) {
            List<String> paths = pathsByType.get(type);
            if (paths != null) {
                for (String path : paths) {
                    Resource altResource = Resource.builder(resource).path(path).build();
                    modelBuilder.addResource(altResource);
                }
                return;
            }
        }

        // no customizations happened... add unchanged
        modelBuilder.addResource(resource);
    }

    @Override
    public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
        // TODO: custom paths for subresources? Suppose less important than overrdiign paths for root resources,
        //  so we can ignore this for now
        return subResourceModel;
    }
}
