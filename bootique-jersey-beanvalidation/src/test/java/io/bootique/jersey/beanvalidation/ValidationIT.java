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

import io.bootique.jersey.JerseyModule;
import io.bootique.test.junit.BQTestFactory;
import org.hibernate.validator.constraints.Range;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ValidationIT {

    @ClassRule
    public static BQTestFactory testFactory = new BQTestFactory();

    private static WebTarget baseTarget = ClientBuilder.newClient().target("http://127.0.0.1:8080/");

    @BeforeClass
    public static void beforeAll() {
        testFactory.app("-s")
                .autoLoadModules()
                .module(b -> JerseyModule.extend(b).addResource(Resource.class))
                .module(b -> JerseyBeanValidationModule.extend(b).sendErrorsInResponse())
                .run();
    }

    @Test
    public void testParamValidation_NotNull() {
        Response ok = baseTarget.path("notNull").queryParam("q", "A").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, ok.getStatus());
        assertEquals("_A_", ok.readEntity(String.class));

        Response missing = baseTarget.path("notNull").request(MediaType.TEXT_PLAIN).get();
        assertEquals(400, missing.getStatus());
        assertEquals("'q' is required (path = Resource.getNotNull.arg0, invalidValue = null)", missing.readEntity(String.class).trim());
    }

    @Test
    public void testParamValidation_Range() {
        Response ok = baseTarget.path("range").queryParam("q", "3").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, ok.getStatus());
        assertEquals("_3_", ok.readEntity(String.class));

        Response outOfRange = baseTarget.path("range").queryParam("q", "2").request(MediaType.TEXT_PLAIN).get();
        assertEquals(400, outOfRange.getStatus());
        assertEquals("'q' is out of range (path = Resource.getRange.arg0, invalidValue = 2)", outOfRange.readEntity(String.class).trim());

        Response missing = baseTarget.path("range").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, missing.getStatus());
        assertEquals("_null_", missing.readEntity(String.class));
    }

    @Test
    public void testParamValidation_ListSize() {
        Response ok = baseTarget.path("size")
                .queryParam("q", "3")
                .queryParam("q", "1")
                .queryParam("q", "8")
                .request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, ok.getStatus());
        assertEquals("_[3, 1, 8]_", ok.readEntity(String.class));

        Response listTooShort = baseTarget.path("size")
                .queryParam("q", "2")
                .request(MediaType.TEXT_PLAIN).get();
        assertEquals(400, listTooShort.getStatus());
        assertEquals("'q' is the wrong size (path = Resource.getSize.arg0, invalidValue = [2])", listTooShort.readEntity(String.class).trim());

        Response missing = baseTarget.path("size")
                .request(MediaType.TEXT_PLAIN).get();
        assertEquals(400, missing.getStatus());
        assertEquals("'q' is the wrong size (path = Resource.getSize.arg0, invalidValue = [])", missing.readEntity(String.class).trim());
    }

    @Test
    public void testParamValidation_Valid() {
        Response ok = baseTarget.path("valid").queryParam("q", "a1").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, ok.getStatus());
        assertEquals("_{a1}_", ok.readEntity(String.class));

        Response badChars = baseTarget.path("valid").queryParam("q", "a*").request(MediaType.TEXT_PLAIN).get();
        assertEquals(400, badChars.getStatus());
        assertEquals("Not an alphanumeric String (path = Resource.getValid.arg0.alphaNum, invalidValue = a*)", badChars.readEntity(String.class).trim());

        Response missing = baseTarget.path("valid").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, missing.getStatus());
        assertEquals("_null_", missing.readEntity(String.class));
    }

    @Test
    public void testParamValidation_Custom() {
        Response ok = baseTarget.path("custom").queryParam("q", "a1").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, ok.getStatus());
        assertEquals("_a1_", ok.readEntity(String.class));

        Response badChars = baseTarget.path("custom").queryParam("q", "b1").request(MediaType.TEXT_PLAIN).get();
        assertEquals(400, badChars.getStatus());
        assertEquals("'q' doesn't start with 'a' (path = Resource.getCustom.arg0, invalidValue = b1)", badChars.readEntity(String.class).trim());

        Response missing = baseTarget.path("custom").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, missing.getStatus());
        assertEquals("_null_", missing.readEntity(String.class));
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
