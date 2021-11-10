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
package io.bootique.jersey.v3.jackson;

import com.fasterxml.jackson.databind.JsonSerializer;
import io.bootique.BQCoreModule;
import io.bootique.ModuleExtender;
import io.bootique.di.Binder;
import io.bootique.di.SetBuilder;

/**
 * @since 2.0
 */
public class JerseyJacksonModuleExtender extends ModuleExtender<JerseyJacksonModuleExtender> {

    private SetBuilder<JsonSerializer> serializers;

    public JerseyJacksonModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public JerseyJacksonModuleExtender initAllExtensions() {
        contributeSerializers();
        return this;
    }

    /**
     * Configures global JSON serialization strategy that would skip properties with null values in responses.
     *
     * @return this extender instance
     */
    public JerseyJacksonModuleExtender skipNullProperties() {
        BQCoreModule.extend(binder).setProperty("bq.jerseyjackson.skipNullProperties", "true");
        return this;
    }

    /**
     * Adds a custom serializer for a value type.
     *
     * @return this extender instance
     * @since 2.0.B1
     */
    public JerseyJacksonModuleExtender addSerializer(Class<? extends JsonSerializer> serializerType) {
        contributeSerializers().add(serializerType);
        return this;
    }

    /**
     * Adds a custom serializer for a value type.
     *
     * @return this extender instance
     * @since 2.0.B1
     */
    public JerseyJacksonModuleExtender addSerializer(JsonSerializer serializer) {
        contributeSerializers().addInstance(serializer);
        return this;
    }

    protected SetBuilder<JsonSerializer> contributeSerializers() {
        if (serializers == null) {
            serializers = newSet(JsonSerializer.class);
        }

        return serializers;
    }
}
