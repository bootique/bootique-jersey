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

import io.bootique.ModuleExtender;
import io.bootique.di.Binder;
import io.bootique.jersey.JerseyModule;
import org.glassfish.jersey.server.ServerProperties;

/**
 * @since 2.0
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class JerseyBeanValidationModuleExtender extends ModuleExtender<JerseyBeanValidationModuleExtender> {

    public JerseyBeanValidationModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public JerseyBeanValidationModuleExtender initAllExtensions() {
        return this;
    }

    public JerseyBeanValidationModuleExtender sendErrorsInResponse() {
        // TODO: should that be configured in YAML, so that the switch can happen between different environments
        //  dynamically?
        JerseyModule.extend(binder).setProperty(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        return this;
    }

}
