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
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


// see https://github.com/bootique/bootique-jersey/issues/11
@BQTest
public class MultiPartFeatureIT {

    static final JettyTester jetty = JettyTester.create();

    @BQApp
    static final BQRuntime app = Bootique.app("-s")
            .autoLoadModules()
            .module(b -> JerseyModule.extend(b).addFeature(MultiPartFeature.class).addResource(Resource.class))
            .module(jetty.moduleReplacingConnectors())
            .createRuntime();

    private WebTarget multiPartTarget = ClientBuilder
            .newBuilder()
            .register(MultiPartFeature.class)
            .build()
            .target(jetty.getUrl());

    @Test
    public void response() {

        FormDataBodyPart part = new FormDataBodyPart("upload", "I am a part", MediaType.TEXT_PLAIN_TYPE);
        FormDataMultiPart multipart = new FormDataMultiPart();
        multipart.bodyPart(part);

        Response r = multiPartTarget
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(multipart, multipart.getMediaType()));

        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("{\"message\":\"I am a part\"}", r.readEntity(String.class));

        r.close();
    }

    @Path("/")
    public static class Resource {

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public Response uploadMultiPart(@FormDataParam("upload") String upload) {
            return Response.ok().entity("{\"message\":\"" + upload + "\"}").build();
        }
    }
}
