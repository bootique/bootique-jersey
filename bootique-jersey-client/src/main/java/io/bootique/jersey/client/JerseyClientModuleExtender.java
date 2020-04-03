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

package io.bootique.jersey.client;

import io.bootique.di.Binder;
import io.bootique.di.SetBuilder;

import javax.ws.rs.core.Feature;

/**
 * @since 0.9
 */
public class JerseyClientModuleExtender {

    private Binder binder;
    private SetBuilder<Feature> features;

    JerseyClientModuleExtender(Binder binder) {
        this.binder = binder;
    }

    JerseyClientModuleExtender initAllExtensions() {
        contributeFeatures();
        return this;
    }


    public JerseyClientModuleExtender addFeature(Feature feature) {
        contributeFeatures().add(feature);
        return this;
    }

    public <T extends Feature> JerseyClientModuleExtender addFeature(Class<T> featureType) {
        contributeFeatures().add(featureType);
        return this;
    }

    protected SetBuilder<Feature> contributeFeatures() {

        if (features == null) {
            features = binder.bindSet(Feature.class, JerseyClientFeatures.class);
        }

        return features;
    }
}
