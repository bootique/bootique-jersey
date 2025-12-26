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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@BQTest
public class ParamConverters_OverridesIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("--server")
            .autoLoadModules()
            .module(b -> JerseyModule.extend(b).addApiResource(Resource.class))
            .module(b -> JerseyModule.extend(b)
                    .addParamConverter(LocalDate.class, MinusOneDateConverter.class)
                    .addParamConverter(LocalTime.class, MinusOneTimeConverter.class)
                    .addParamConverter(LocalDateTime.class, MinusOneDateTimeConverter.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    @Test
    public void dateConverter() {
        Response r = jetty.getTarget().path("date/2020-02-03").request().get();
        JettyTester.assertOk(r).assertContent("[2020-02-02]");
    }

    @Test
    public void timeConverter() {
        Response r = jetty.getTarget().path("time/12:00:02").request().get();
        JettyTester.assertOk(r).assertContent("[12:00:01]");
    }

    @Test
    public void dateTimeConverter() {
        Response r = jetty.getTarget().path("datetime/2020-02-03T12:00:02").request().get();
        JettyTester.assertOk(r).assertContent("[2020-02-02T12:00:01]");
    }

    @Path("/")
    public static class Resource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("date/{date}")
        public String get(@PathParam("date") LocalDate date) {
            return "[" + date + "]";
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("time/{time}")
        public String getT(@PathParam("time") LocalTime time) {
            return "[" + time + "]";
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("datetime/{datetime}")
        public String getDT(@PathParam("datetime") LocalDateTime dt) {
            return "[" + dt + "]";
        }
    }

    static class MinusOneDateConverter implements ParamConverter<LocalDate> {

        @Override
        public LocalDate fromString(String value) {
            return value != null ? LocalDate.parse(value).minusDays(1) : null;
        }

        @Override
        public String toString(LocalDate value) {
            return value.toString();
        }
    }

    static class MinusOneTimeConverter implements ParamConverter<LocalTime> {

        @Override
        public LocalTime fromString(String value) {
            return value != null ? LocalTime.parse(value).minusSeconds(1) : null;
        }

        @Override
        public String toString(LocalTime value) {
            return value.toString();
        }
    }

    static class MinusOneDateTimeConverter implements ParamConverter<LocalDateTime> {

        @Override
        public LocalDateTime fromString(String value) {
            return value != null ? LocalDateTime.parse(value).minusSeconds(1).minusDays(1) : null;
        }

        @Override
        public String toString(LocalDateTime value) {
            return value.toString();
        }
    }
}
