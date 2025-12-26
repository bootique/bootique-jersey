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
package io.bootique.jersey.beanvalidation;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.validation.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.validator.constraints.Range;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@BQTest
public class ValidationIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> JerseyModule.extend(b).addApiResource(Resource.class))
            .module(b -> JerseyBeanValidationModule.extend(b).sendErrorsInResponse())
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    private static Consumer<String> assertTrimmed(String expected) {
        return c -> {
            assertNotNull(c);
            assertEquals(expected, c.trim());
        };
    }

    @Test
    public void paramValidation_NotNull() {
        Response ok = jetty.getTarget().path("notNull").queryParam("q", "A").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertOk(ok).assertContent("_A_");

        Response missing = jetty.getTarget().path("notNull").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertBadRequest(missing)
                .assertContent(assertTrimmed("'q' is required (path = Resource.getNotNull.arg0, invalidValue = null)"));
    }

    @Test
    public void paramValidation_Range() {
        Response ok = jetty.getTarget().path("range").queryParam("q", "3").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertOk(ok).assertContent("_3_");

        Response outOfRange = jetty.getTarget().path("range").queryParam("q", "2").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertBadRequest(outOfRange)
                .assertContent(assertTrimmed("'q' is out of range (path = Resource.getRange.arg0, invalidValue = 2)"));

        Response missing = jetty.getTarget().path("range").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertOk(missing).assertContent("_null_");
    }

    @Test
    public void paramValidation_ListSize() {
        Response ok = jetty.getTarget().path("size")
                .queryParam("q", "3")
                .queryParam("q", "1")
                .queryParam("q", "8")
                .request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertOk(ok).assertContent("_[3, 1, 8]_");

        Response listTooShort = jetty.getTarget().path("size")
                .queryParam("q", "2")
                .request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertBadRequest(listTooShort)
                .assertContent(assertTrimmed("'q' is the wrong size (path = Resource.getSize.arg0, invalidValue = [2])"));

        Response missing = jetty.getTarget().path("size").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertBadRequest(missing)
                .assertContent(assertTrimmed("'q' is the wrong size (path = Resource.getSize.arg0, invalidValue = [])"));
    }

    @Test
    public void paramValidation_Valid() {
        Response ok = jetty.getTarget().path("valid").queryParam("q", "a1").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertOk(ok).assertContent("_{a1}_");

        Response badChars = jetty.getTarget().path("valid").queryParam("q", "a*").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertBadRequest(badChars)
                .assertContent(assertTrimmed("Not an alphanumeric String (path = Resource.getValid.arg0.alphaNum, invalidValue = a*)"));

        Response missing = jetty.getTarget().path("valid").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertOk(missing).assertContent("_null_");
    }

    @Test
    public void paramValidation_Custom() {
        Response ok = jetty.getTarget().path("custom").queryParam("q", "a1").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertOk(ok).assertContent("_a1_");

        Response badChars = jetty.getTarget().path("custom").queryParam("q", "b1").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertBadRequest(badChars)
                .assertContent(assertTrimmed("'q' doesn't start with 'a' (path = Resource.getCustom.arg0, invalidValue = b1)"));

        Response missing = jetty.getTarget().path("custom").request(MediaType.TEXT_PLAIN).get();
        JettyTester.assertOk(missing).assertContent("_null_");
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = CustomParamValidator.class)
    @Documented
    public @interface CustomValidation {

        String message() default "...";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    @Path("/")
    public static class Resource {

        @GET
        @Path("notNull")
        public Response getNotNull(@NotNull(message = "'q' is required") @QueryParam("q") String q) {
            return Response.ok("_" + q + "_", MediaType.TEXT_PLAIN_TYPE).build();
        }

        @GET
        @Path("range")
        public Response getRange(@Range(min = 3, max = 6, message = "'q' is out of range") @QueryParam("q") Integer q) {
            return Response.ok("_" + q + "_", MediaType.TEXT_PLAIN_TYPE).build();
        }

        @GET
        @Path("size")
        public Response getSize(@Size(min = 3, max = 6, message = "'q' is the wrong size") @QueryParam("q") List<Integer> q) {
            return Response.ok("_" + q + "_", MediaType.TEXT_PLAIN_TYPE).build();
        }

        @GET
        @Path("valid")
        public Response getValid(@Valid @QueryParam("q") ParamHolder q) {
            return Response.ok("_" + q + "_", MediaType.TEXT_PLAIN_TYPE).build();
        }

        @GET
        @Path("custom")
        public Response getCustom(@CustomValidation(message = "'q' doesn't start with 'a'") @QueryParam("q") String q) {
            return Response.ok("_" + q + "_", MediaType.TEXT_PLAIN_TYPE).build();
        }
    }

    public static class ParamHolder {

        @Pattern(regexp = "^[A-Za-z0-9]*$", message = "Not an alphanumeric String")
        private String alphaNum;

        public ParamHolder(String alphaNum) {
            this.alphaNum = alphaNum;
        }

        public String getAlphaNum() {
            return alphaNum;
        }

        @Override
        public String toString() {
            return "{" + alphaNum + "}";
        }
    }

    public static class CustomParamValidator implements ConstraintValidator<CustomValidation, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.startsWith("a");
        }
    }
}
