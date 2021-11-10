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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

import java.util.Set;

/**
 * @since 2.0
 */
@BQConfig
public class JerseyJacksonFactory {

    private boolean skipNullProperties;

    @BQConfigProperty
    public void setSkipNullProperties(boolean skipNullProperties) {
        this.skipNullProperties = skipNullProperties;
    }

    public ObjectMapperResolver createObjectMapperResolver(Set<JsonSerializer> serializers) {
        return new ObjectMapperResolver(createObjectMapper(serializers));
    }

    protected ObjectMapper createObjectMapper(Set<JsonSerializer> serializers) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        if (!serializers.isEmpty()) {
            SimpleModule m = new SimpleModule("BQJerseyJacksonSerializers", Version.unknownVersion());
            serializers.forEach(m::addSerializer);

            mapper.registerModule(m);
        }

        if (skipNullProperties) {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

        return mapper;
    }
}
