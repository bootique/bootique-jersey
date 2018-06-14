/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.jersey;

import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import static org.junit.Assert.assertTrue;

public class CustomDynamicFeaturesIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testFeaturesLoaded() {

        testFactory.app("-s")
                .module(b -> JerseyModule
                        .extend(b)
                        .addDynamicFeature(DynamicFeature1.class)
                        .addDynamicFeature(DynamicFeature2.class))
                .run();

        assertTrue(DynamicFeature1.LOADED);
        assertTrue(DynamicFeature2.LOADED);
    }

    static class DynamicFeature1 implements DynamicFeature {

        static boolean LOADED = false;

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            LOADED = true;
        }
    }

    static class DynamicFeature2 implements DynamicFeature {

        static boolean LOADED = false;

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            LOADED = true;
        }
    }

}
