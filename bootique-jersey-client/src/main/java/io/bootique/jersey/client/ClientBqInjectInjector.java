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

import io.bootique.di.BQInject;
import io.bootique.di.Injector;
import io.bootique.di.TypeLiteral;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class ClientBqInjectInjector extends BaseClientBqHk2Bridge implements InjectionResolver<BQInject> {

    @Inject
    public ClientBqInjectInjector(Injector injector) {
        super(injector);
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return true;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> serviceHandle) {
        TypeLiteral<?> typeLiteral = TypeLiteral.of(injectee.getRequiredType());
        boolean isProvider = javax.inject.Provider.class.equals(typeLiteral.getRawType());

        Provider<?> provider = resolveBqProvider(injectee);
        return isProvider ? provider : provider.get();
    }

}
