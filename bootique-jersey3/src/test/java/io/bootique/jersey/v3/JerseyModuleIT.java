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

package io.bootique.jersey.v3;

import io.bootique.BQRuntime;
import io.bootique.di.Key;
import io.bootique.di.TypeLiteral;
import io.bootique.jetty.v11.MappedServlet;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@BQTest
public class JerseyModuleIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testDefaultContents() {
        BQRuntime runtime = testFactory.app().createRuntime();

        assertNotNull(runtime.getInstance(ResourceConfig.class));

        TypeLiteral<MappedServlet<ServletContainer>> jerseyServletKey = new TypeLiteral<MappedServlet<ServletContainer>>() {
        };

        assertNotNull(runtime.getInstance(Key.get(jerseyServletKey)));
    }

    @Test
    public void testProperties() {

        BQRuntime runtime = testFactory.app()
                .autoLoadModules()
                .module(b -> JerseyModule.extend(b).setProperty("test.x", 67))
                .createRuntime();

        ResourceConfig config = runtime.getInstance(ResourceConfig.class);
        assertEquals(67, config.getProperty("test.x"));
    }

    @Test
    public void testNoResourcesModule() {
        BQRuntime runtime = testFactory.app().module(JerseyModule.class).createRuntime();
        assertNotNull(runtime.getInstance(ResourceConfig.class));
    }
}
